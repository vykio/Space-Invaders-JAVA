/*
    Alexandre VASSEUR ING1 - Groupe TP2
*/
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SpaceInvaders extends Application {

    final double WIDTH=1000, HEIGHT=800;
    final int TAILLE_JOUEUR_X = 24+12;
    final int TAILLE_JOUEUR_Y = 18+9;
    final int COIN_PER_WIN = 20;

    String saveFile="save.sav";

    InputStream input = this.getClass().getResourceAsStream("/imgs/player.png");
    InputStream enemy1img = this.getClass().getResourceAsStream("/imgs/enemy1.png");
    InputStream enemy2img = this.getClass().getResourceAsStream("/imgs/enemy12.png");
    InputStream enemy3img = this.getClass().getResourceAsStream("/imgs/enemy13.png");
    InputStream coinimage = this.getClass().getResourceAsStream("/imgs/coin.png");

    Image JOUEUR_IMG = new Image(input);
    Image COIN_IMG = new Image(coinimage);
    Image ENEMY1_IMG = new Image(enemy3img);
    Image ENEMY2_IMG = new Image(enemy2img);
    Image ENEMY3_IMG = new Image(enemy3img);

    int nbVie = 3;
    int score = 0;
    int level=1;
    int best_score=0;
    int best_level=1;
    int scoreTotal=0;
    int nbPartiesJouees=0;
    int coins=0;
    Object[][] ameliorations = {
            /* {Nom de l'amélioration, Niveau actuel, Prix de base (niveau=1)} */
            {"Vitesse des tirs", 1, 200},
            {"Nombre de missiles", 1 , 500}
    };

    /* Pour éviter de spam-click la touche de tir = un tir à la fois */
    boolean pressFire = false;

    /* STATES */
    public int GAME_STATE = 0; // 0 : menu, 1: ingame, ...
    public int GAME_KEY = 0; //1: left, 2:up, 3:right, 4:down, 5:enter, 6:escape, 0:NONE
    int MUSIC_STATE = 3; //menu:0 ; game:1; none:3

    /* Menu initialisation */
    public String[] menuInfo = {"Nouvelle partie","Boutique","Stats","A Propos","Quitter"};
    int indiceActuel=0;
    int indiceActuelShop=0;
    boolean pressKeyMenu=false;
    boolean pressKeyShop=false;

    /* Elements d'affichage */
    private Canvas canvas;
    private Pane root;
    private Scene scene;
    private GraphicsContext gc;

    /* Game objects */
    private Fusee joueur;
    private List<Tir> tirs;
    private List<Alien> aliens;
    Alien plusAGauche;
    Alien plusADroite;
    Timeline timeline;
    Clip clip;


    public void playSound(final String url) {
        try {
            clip = AudioSystem.getClip();

            InputStream audioSrc = this.getClass().getResourceAsStream("/music/" + url);
            InputStream bufferedIn = new BufferedInputStream(audioSrc); // pour jdk14 = obliger d'utiliser un buffer pour lire la musique

            AudioInputStream inputStream = AudioSystem.getAudioInputStream(bufferedIn);
            clip.open(inputStream);

            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-10.0f); // Baisse le volume d'un gain de 10

            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void stopSound() {
        try {
            clip.stop();
        } catch (Exception e) {
            //System.out.println("Pas de son");
        }
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return (double) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    /* Obtenir l'alien ayant le X le plus faible */
    Alien getAlienMinX(List<Alien> aliens) {
        if (aliens.size() != 0) {
            double minX = aliens.get(0).posX;
            Alien output = aliens.get(0);
            for (int i = 0; i < aliens.size(); i++) {
                if (aliens.get(i).posX < minX) {
                    minX = aliens.get(i).posX;
                    output = aliens.get(i);
                }
            }
            return output;
        } else {
            return null;
        }
    }

    /* Obtenir l'alien ayant le Y le plus grand = + proche du joueur */
    Alien getAlienMaxEnY(List<Alien> aliens) {
        if (aliens.size() != 0) {
            double maxY = aliens.get(0).posY;
            Alien output = aliens.get(0);
            for (int i = 0; i < aliens.size(); i++) {
                if (aliens.get(i).posY > maxY) {
                    maxY = aliens.get(i).posY;
                    output = aliens.get(i);
                }
            }
            return output;
        } else {
            return null;
        }
    }

    /* Obtenir l'alien ayant le X le plus grand */
    Alien getAlienMaxX(List<Alien> aliens) {
        if (aliens.size() != 0) {
            double maxX = aliens.get(0).posX;
            Alien output = aliens.get(0);
            for (int i = 0; i<aliens.size(); i++) {
                if (aliens.get(i).posX > maxX) {
                    maxX = aliens.get(i).posX;
                    output = aliens.get(i);
                }
            }
            return output;
        } else {
            return null;
        }

    }

    /* Renvoie une liste d'aliens n'ayant PAS comme coordonnée X, celle entrée en paramètre */
    List<Alien> supprimerAlienPourXEgaleA(List<Alien> aliens, double PositionX) {
        List<Alien> list = aliens;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).posX == PositionX) {
                    list.remove(i);
                }
            }
            return list;
    }

    /* Avoir la liste des aliens les plus bas de chaque colonne = ceux qui peuvent tirer */
    List<Alien> getAliensPlusBas(List<Alien> aliens) {
        List<Alien> blank = new ArrayList<Alien>();
        if (aliens.size() != 0) {
            List<Alien> input = new ArrayList<Alien>(aliens);
            List<Alien> output = new ArrayList<Alien>();
            Alien temp;
            for (int i = 0; i < input.size(); i++) {
                temp = getAlienMaxEnY(input);
                input = supprimerAlienPourXEgaleA(input, temp.posX);
                output.add(temp);
                input.remove(temp);
            }
            return output;
        } return blank;
    }

    /* Fonction lancée à chaque "tick" par le timeline, et gère quelle fonction appeler en fonction des états (GAME_STATE) */
    public void gameHandler() {
        //System.out.println(GAME_KEY);
        switch (GAME_STATE){
            case 0: //MENU
                scene.setOnKeyPressed(e -> {
                    switch (e.getCode()) {
                        case UP:
                            if (!pressKeyMenu) {
                                if (indiceActuel == 0) {
                                    indiceActuel = menuInfo.length-1;
                                    //System.out.println(menuInfo.length-1);
                                } else {
                                    indiceActuel--;
                                }
                                pressKeyMenu = true;
                                break;
                            }

                        case DOWN:
                            if (!pressKeyMenu) {
                                if (indiceActuel == menuInfo.length-1) {
                                    indiceActuel = 0;
                                } else {
                                    indiceActuel++;
                                }
                                pressKeyMenu =true;
                                break;
                            }
                        case ENTER:
                            if (!pressKeyMenu) {
                                GAME_KEY = 5;
                                pressKeyMenu = true;
                                break;
                            }
                    }
                });

                scene.setOnKeyReleased(e -> {
                    switch (e.getCode()) {
                        case UP:
                        case DOWN:
                        case ENTER:
                            GAME_KEY=0;
                            pressKeyMenu = false;
                            break;
                    }
                });
                if (MUSIC_STATE != GAME_STATE) {
                    MUSIC_STATE = GAME_STATE;
                    stopSound();
                    playSound("menu.wav");
                }
                menu();
                break;
            case 1: //EN JEU
                scene.setOnKeyPressed(e -> {
                    switch (e.getCode()) {
                        case LEFT:
                            joueur.moveLeft(0.5);
                            break;
                        case RIGHT:
                            joueur.moveRight(0.5);
                            break;
                        case UP:
                            if (!pressFire) {
                                pressFire = true;
                                for (int i = 0; i < (int)ameliorations[1][1]; i++) {
                                    tirs.add(new Tir(joueur.posX+joueur.sizeX/2, joueur.posY-i*20));
                                }

                            }
                            break;
                        /*case R:
                            level=1;
                            score=0;
                            creerContenu(true);
                            break;*/
                        case ESCAPE:
                            scoreTotal += score;
                            writeSavedData();
                            level=1;
                            score=0;
                            nbVie=3;
                            GAME_STATE = 0;
                            //System.out.println("gameHandler>1>ESCAPE");
                            break;
                    }
                });

                scene.setOnKeyReleased(e -> {
                    switch (e.getCode()) {
                        case LEFT:
                            joueur.moveLeft(0);
                            break;
                        case RIGHT:
                            joueur.moveRight(0);
                            break;
                        case UP:
                            pressFire = false;
                            break;
                    }
                });
                if (MUSIC_STATE != GAME_STATE) {
                    MUSIC_STATE = GAME_STATE;
                    stopSound();
                    playSound("level12.wav");
                }
                run();
                break;
            case 2: //A PROPOS
                scene.setOnKeyPressed(e -> {
                            switch (e.getCode()) {
                                case ESCAPE:
                                    GAME_STATE = 0;
                                    //System.out.println("gameHandler>2>ESCAPE");
                                    break;
                            }
                        });
                aPropos();
                break;
            case 3: //STATISTIQUES
                scene.setOnKeyPressed(e -> {
                    switch (e.getCode()) {
                        case ESCAPE:
                            GAME_STATE = 0;
                            //System.out.println("gameHandler>3>ESCAPE");
                            break;
                    }
                });
                stats();
                break;
            case 4:
                scene.setOnKeyPressed(e -> {
                    switch (e.getCode()) {
                        case UP:
                            if (!pressKeyShop) {
                                if (indiceActuelShop == 0) {
                                    indiceActuelShop = ameliorations.length-1;
                                    //System.out.println(menuInfo.length-1);
                                } else {
                                    indiceActuelShop--;
                                }
                                pressKeyShop = true;
                                break;
                            }

                        case DOWN:
                            if (!pressKeyShop) {
                                if (indiceActuelShop == ameliorations.length-1) {
                                    indiceActuelShop = 0;
                                } else {
                                    indiceActuelShop++;
                                }
                                pressKeyShop =true;
                                break;
                            }
                        case ENTER:
                            if (!pressKeyShop) {
                                GAME_KEY = 5;
                                pressKeyShop = true;
                                break;
                            }
                        case ESCAPE:
                            GAME_STATE = 0;
                            //System.out.println("gameHandler>3>ESCAPE");
                            break;
                    }
                });

                scene.setOnKeyReleased(e -> {
                    switch (e.getCode()) {
                        case UP:
                        case DOWN:
                        case ENTER:
                            pressKeyShop = false;
                            break;
                    }
                });
                shop();
                break;
        }
    }

    /* Fenêtre Boutique */
    public void shop() {
        pressKeyMenu=false;
        gc.clearRect(0,0,WIDTH, HEIGHT);
        gc.setFill(Color.grayRgb(20));
        gc.fillRect(0,0, WIDTH, HEIGHT);

        gc.setFill(Color.WHITE);

        gc.setFont(new Font(50));
        gc.fillText("Boutique", WIDTH/2, 100, 200);

        gc.setFont(new Font(10));
        gc.fillText("Appuyez sur ECHAP pour retourner au menu", WIDTH/2, 150, 700);

        gc.setFont(new Font(30));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.drawImage(COIN_IMG, 50, 60, 40, 40);
        gc.fillText(""+coins, 50+60, 60+20, 700);

        gc.setTextAlign(TextAlignment.CENTER);

        for (int i = 0; i<ameliorations.length ; i++) {
            int cout = (int)ameliorations[i][2]*(int)ameliorations[i][1];
            boolean achatipossible = (coins - cout >= 0);

            if (indiceActuelShop == i) {

                if (GAME_KEY == 5) {
                    //System.out.println("shop>GAMEKEY");

                    if (cout <= coins) {
                        System.out.println(cout);
                        coins-=cout;
                        GAME_KEY=0;
                        ameliorations[i][1] = (int)ameliorations[i][1] + 1;
                        writeSavedData();
                    }


                }

                gc.setFont(new Font(35));
                if (achatipossible) {
                    gc.setFill(Color.ORANGE);
                } else {
                    gc.setFill(Color.GRAY);
                }

                gc.fillText("+ " + "[Lvl."+ ameliorations[i][1] +"] " + ameliorations[i][0] + " ("+ ((int)ameliorations[i][2]*(int)ameliorations[i][1]) +" pièces)", WIDTH / 2, HEIGHT / 2 + 50 * i, 700);
            } else {
                gc.setFont(new Font(25));
                if (achatipossible) {
                    gc.setFill(Color.WHITE);
                } else {
                    gc.setFill(Color.GRAY);
                }

                gc.fillText(""+ "[Lvl."+ ameliorations[i][1] +"] " + ameliorations[i][0], WIDTH / 2, HEIGHT / 2 + 50 * i, 700);
            }
        }
    }

    /* Fenêtre A Propos */
    public void aPropos() {
        /* Obligation de rafraichir le canvas car cette fonction est appelée sans cesse */
        gc.clearRect(0,0,WIDTH, HEIGHT);
        gc.setFill(Color.grayRgb(20));
        gc.fillRect(0,0, WIDTH, HEIGHT);

        gc.setFill(Color.WHITE);

        gc.setFont(new Font(50));
        gc.fillText("A Propos", WIDTH/2, 100, 200);

        gc.setFont(new Font(10));
        gc.fillText("Appuyez sur ECHAP pour retourner au menu", WIDTH/2, 150, 700);

        gc.setFont(new Font(20));
        gc.fillText("Codeur : Alexandre Vasseur", WIDTH/2, 350, 700);
        gc.fillText("Projet de Programmation Orientée Objet (JAVA) de ING1 (2019-2020)", WIDTH/2, 390, 700);

        gc.setFont(new Font(30));
        gc.setFill(Color.ORANGE);
        gc.fillText("Musique:", WIDTH/2, 450, 700);
        gc.setFill(Color.WHITE);
        gc.fillText("Menu: Sample de Enigmatic (www.bensound.com)", WIDTH/2, 500, 700);
        gc.fillText("En jeu: Loop style retro, par Tarkowski T.", WIDTH/2, 540, 700);
    }

    /* Fenêtre Stats */
    public void stats() {
        gc.clearRect(0,0,WIDTH, HEIGHT);
        gc.setFill(Color.grayRgb(20));
        gc.fillRect(0,0, WIDTH, HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(new Font(50));
        gc.fillText("Statistiques", WIDTH/2, 100, 400);

        gc.setFont(new Font(10));
        gc.fillText("Appuyez sur ECHAP pour retourner au menu", WIDTH/2, 150, 700);

        gc.setFont(new Font(30));
        gc.setFill(Color.WHITE);
        gc.fillText("Meilleur score :", WIDTH/2, 320, 700);
        gc.setFont(new Font(50));
        gc.setFill(Color.ORANGE);
        gc.fillText(""+best_score, WIDTH/2, 360, 700);

        gc.setFont(new Font(30));
        gc.setFill(Color.WHITE);
        gc.fillText("Niveau max :", WIDTH/2, 420, 700);
        gc.setFont(new Font(50));
        gc.setFill(Color.ORANGE);
        gc.fillText(""+best_level, WIDTH/2, 460, 700);

        gc.setFont(new Font(30));
        gc.setFill(Color.WHITE);
        gc.fillText("Score Total :", WIDTH/2, 520, 700);
        gc.setFont(new Font(50));
        gc.setFill(Color.ORANGE);
        gc.fillText(""+scoreTotal, WIDTH/2, 560, 700);

        gc.setFont(new Font(30));
        gc.setFill(Color.WHITE);
        gc.fillText("Nombre de parties jouées :", WIDTH/2, 620, 700);
        gc.setFont(new Font(50));
        gc.setFill(Color.ORANGE);
        gc.fillText(""+nbPartiesJouees, WIDTH/2, 660, 700);

    }

    /* Fenêtre GameOver */
    public void gameover() {
        joueur.velX=0; //Eviter un bug
        joueur.detruit = true;

        /* Rafraichissement du canvas */
        gc.clearRect(0,0,WIDTH, HEIGHT);
        gc.setFill(Color.grayRgb(20));
        gc.fillRect(0,0, WIDTH, HEIGHT);

        /* Affichage Texte */
        gc.setFill(Color.WHITE);
        gc.setFont(new Font(40));
        gc.fillText("GAMEOVER", WIDTH/2, HEIGHT/2-100, 200);
        gc.setFont(new Font(25));
        gc.fillText("Appuyez sur ECHAP pour retourner au menu", WIDTH/2, HEIGHT/2+80, 700);
    }

    /* Fenêtre Win */
    public void win () {
        timeline.pause();
        joueur.velX=0;
        joueur.detruit = true;
        joueur.gagneAvant=true;

        /* Enregistrement du meilleur score, meilleur niveau et des pièces */
        best_score = Math.max(best_score, score);
        best_level = Math.max(best_level, level);
        int coins_gained=COIN_PER_WIN+10*(level-1);
        coins+=coins_gained;
        writeSavedData();

        gc.clearRect(0,0,WIDTH, HEIGHT);
        gc.setFill(Color.grayRgb(20));
        gc.fillRect(0,0, WIDTH, HEIGHT);

        gc.setFill(Color.WHITE);

        gc.setFont(new Font(40));
        gc.fillText("Vous avez gagné !", WIDTH/2, HEIGHT/2-100, 500);
        gc.setFill(Color.ORANGE);
        gc.fillText("Score: "+score +"   |   Niveau: "+level, WIDTH/2, HEIGHT/2-55, 500);

        gc.setFont(new Font(25));
        gc.setFill(Color.WHITE);
        gc.fillText("Lancement du niveau suivant dans 5 secondes...", WIDTH/2, HEIGHT/2+30, 500);
        gc.fillText("Appuyez sur ECHAP pour retourner au menu après les 5 secondes", WIDTH/2, HEIGHT/2+80, 1000);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFont(new Font(30));
        gc.fillText("+"+coins_gained, WIDTH/2, HEIGHT/2+150+20, 1000);
        gc.drawImage(COIN_IMG, WIDTH/2+20, HEIGHT/2+150, 40, 40);
        gc.setTextAlign(TextAlignment.CENTER);

        /* Pour faire patienter le canvas 5 secondes
        * Le sleep() ne fonctionnait pas... */
        Task<Void> sleeper = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                return null;
            }
        };
        /* Une fois que les 5 secondes sont passées */
        sleeper.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                joueur.gagneAvant = false;
                //score = best_score;
                level++;
                joueur.detruit=false;

                reinitAliensTirs();

                timeline.play();
            }
        });
        new Thread(sleeper).start();

    }

    /* Permet d'initialiser les tirs et les aliens (Placement des aliens) */
    public void reinitAliensTirs () {
        tirs = new ArrayList<Tir>();
        aliens = new ArrayList<Alien>();

        for (int j =0; j<5; j++) {
            for (int k =0 ; k <11; k++){
                aliens.add(new Alien(25+k*TAILLE_JOUEUR_X+25*k, j*TAILLE_JOUEUR_Y+100+10*j, TAILLE_JOUEUR_X, TAILLE_JOUEUR_Y, ENEMY1_IMG));
            }
        }
    }


    /* Le jeu en lui même */
    private void run () {

        //System.out.println("run");
        //System.out.println("GameState:"+GAME_STATE);

        plusAGauche = getAlienMinX(aliens);
        plusADroite = getAlienMaxX(aliens);
        List<Alien> aliensBottom = getAliensPlusBas(aliens);


        for (int i=0;i<aliensBottom.size();i++) {

            /* Le tir des ennemis est fait par rapport à un nombre aléatoire, si ce nombre est inférieur à un certain nombre, l'alien en question tir */
            double tempRnd = 0.0001*5*11/aliens.size();
            if (tempRnd > 0.0015+0.001*(level-1)*(level)) tempRnd = 0.0015+0.001*(level-1)*(level);
            if (Math.random() < tempRnd) {
                tirs.add(new Tir(aliensBottom.get(i).posX + aliensBottom.get(i).sizeX / 2, aliensBottom.get(i).posY, 1, true));
            }

            /* Si l'alien le plus bas est passé en dessous d'un certain seuil, nous avons perdu */
            if (aliensBottom.get(i).posY > HEIGHT-2*TAILLE_JOUEUR_Y) {
                nbVie=0;
            }
        }

        /* Descente des aliens */
        if (plusADroite!=null && ((plusAGauche.posX+plusAGauche.velX < 25) || (plusADroite.posX+plusADroite.velX > WIDTH-25-plusADroite.sizeX))) {
            if (aliens.size() != 0) Alien.direction = Alien.direction*-1;
            for (int i=0; i<aliens.size();i++){
                aliens.get(i).posY += aliens.get(i).sizeY; // Ils descendent d'une fois leur taille
            }
        }

        /* Changement de vitesse en fonction des aliens restants */
        for (Alien alien : aliens) {
            double temp_vitesse = 0.075*(5*11)/(aliens.size())+0.05*level;
            if (temp_vitesse > 1.2) {
                temp_vitesse = 1.2;
            }
            Alien.vitesse = temp_vitesse;
            alien.update();
        }

        /* Pour chaque tirs */
        for (int i = 0; i< tirs.size(); i++) {

            /* Mise à jour de leur position */
            tirs.get(i).vitesse += 0.00025*(int)ameliorations[0][1];
            tirs.get(i).update();

            if ((tirs.get(i).getPosX() < -10) || (tirs.get(i).getPosX() > WIDTH+10) || (tirs.get(i).getPosY() < -10) || (tirs.get(i).getPosY() > HEIGHT + 10)) {
                /* Si le tir est en dehors du cadre du jeu, on le supprime */
                tirs.remove(i);
            } else {
                /* Si le tir est fait par nous */
                if (!tirs.get(i).isByEnemy) {
                    for (int j = 0; j< aliens.size(); j++) {
                        if (tirs.get(i).collision(aliens.get(j))) {
                            /* Nous supprimons ce tir et l'alien qui l'a touché. Nous augmentons le score */
                            tirs.remove(i);
                            aliens.remove(j);
                            score += 50+50*level;
                            break;
                        }
                    }
                } else {
                    /* Si le tir est fait par un alien */
                    if (joueur.collision(tirs.get(i))) {
                        tirs.remove(i);
                        if (nbVie!=0) {
                            /* Mise à jour de la position du joueur */
                            joueur.posX=WIDTH/2;
                            joueur.posY=HEIGHT-TAILLE_JOUEUR_Y-10;
                            /* On perd une vie */
                            nbVie--;
                        }

                        break;
                    }
                }

            }

        }

        /* Une fois que la partie update est faite, nous rafraichissons le canvas pour ensuite afficher */
        gc.clearRect(0,0,WIDTH, HEIGHT);
        gc.setFill(Color.grayRgb(20));
        gc.fillRect(0,0, WIDTH, HEIGHT);

        /* Le joueur est capable de bouger dans les limites du cadre du jeu */
        if ((joueur.posX+joueur.velX >= 25) && (joueur.posX+joueur.velX <= WIDTH-25-joueur.sizeX)) {
            joueur.update();
        }

        /* Affichage du score, niveau et du nombre de vies */
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(Color.WHITE);
        gc.setFont(new Font(20));
        gc.fillText("Score: "+score, WIDTH/2, 50, 200);
        gc.fillText("Niveau: "+level, 100, 50, 200);
        for (int i =0;i<nbVie;i++){
            gc.drawImage(JOUEUR_IMG, WIDTH-200+i*40, 50-TAILLE_JOUEUR_Y+10, TAILLE_JOUEUR_X, TAILLE_JOUEUR_Y);
        }

        /* Affichage du joueur */
        joueur.affiche(gc);

        /* Affichage des tirs */
       for (int i = 0; i< tirs.size(); i++) {
            tirs.get(i).affiche(gc);
        }

       /* Affichage des aliens */
        for (int i = 0; i< aliens.size(); i++) {
            aliens.get(i).affiche(gc);
        }

        /* Si le nombre de vie est nul, nous avons perdu */
        if (nbVie == 0) {
            gameover();
        }

        /* Si il n'y a plus d'aliens, nous avons gagné ce niveau */
        if (aliens.size() == 0) {
            win();
        }
    }

    /* Fenêtre Menu */
    private void menu() {

        joueur.velX=0;
        joueur.detruit = true;

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.clearRect(0,0,WIDTH, HEIGHT);
        gc.setFill(Color.grayRgb(20));
        gc.fillRect(0,0, WIDTH, HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(new Font(80));
        gc.fillText("Space Invaders", WIDTH/2, 250, 700);

        gc.setFont(new Font(30));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.drawImage(COIN_IMG, 50, 60, 40, 40);
        gc.fillText(""+coins, 50+60, 60+20, 700);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(new Font(20));

        for (int i = 0; i<menuInfo.length ; i++) {
            if (indiceActuel == i) {
                if (GAME_KEY == 5) {
                    //System.out.println("menu>GAMEKEY");
                    switch (indiceActuel) {
                        case 0:
                            GAME_STATE = 1;
                            GAME_KEY=0;
                            score=0;
                            level=1;
                            nbPartiesJouees++;
                            writeSavedData();

                            reinitAliensTirs();

                            pressKeyMenu=false;
                            break;
                        case 1:
                            GAME_STATE=4;
                            GAME_KEY=0;
                            break;
                        case 2:
                            GAME_STATE=3; //STATS
                            GAME_KEY=0;
                            break;
                        case 3:
                            GAME_STATE = 2; //a propos
                            GAME_KEY=0;
                            break;
                        case 4:
                            Platform.exit();
                            System.exit(0);
                            break;
                    }
                }

                gc.setFont(new Font(35));
                gc.setFill(Color.ORANGE);
                gc.fillText("> "+menuInfo[i], WIDTH/2, HEIGHT/2+90+50*i, 700);
            } else {
                gc.setFont(new Font(25));
                gc.setFill(Color.WHITE);
                gc.fillText( menuInfo[i], WIDTH/2, HEIGHT/2+90+50*i, 700);
            }

        }



    }

    /* Initialisation du jeu */
    private void creerContenu(boolean restart) {
        //System.out.println("creerContenu");
        if (!restart){
            //System.out.println("creerContenu>if!restart");
            canvas = new Canvas(WIDTH, HEIGHT);
            gc = canvas.getGraphicsContext2D();

            root = new Pane(canvas);
            root.setPrefSize(WIDTH, HEIGHT);

            scene = new Scene(root);

            gc.setFill(Color.grayRgb(20));
            gc.fillRect(0,0, WIDTH, HEIGHT);

            joueur = new Fusee(WIDTH/2, HEIGHT-TAILLE_JOUEUR_Y-10, TAILLE_JOUEUR_X, TAILLE_JOUEUR_Y, JOUEUR_IMG);


        }

        nbVie=3;

        if (restart){
            joueur.velX=0;
        }

        reinitAliensTirs();

        if (!restart || joueur.detruit) {
            //System.out.println("creerContenu>timelinecreate");
            timeline = new Timeline (new KeyFrame(Duration.millis(1), e -> gameHandler()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            joueur.detruit = false;
        }

    }

    /* Crée le fichier de sauvegarde */
    public void initSaveFile() {
        File file = new File(saveFile);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Retourne vrai ou faux si le fichier de sauvegarde existe ou non */
    public boolean saveFileExists() {
        File file = new File(saveFile);

        /* Si le fichier de sauvegarde n'existe pas */
        if (!file.exists()) {
            return false;
        } else {
            return true;
        }
    }

    /* Ecrire dans le fichier de sauvegarde */
    public void writeSavedData() {

        if (!saveFileExists()) initSaveFile();

        // Ecrire dans le fichier
        try(FileOutputStream fileOutputStream = new FileOutputStream(saveFile)) {
            String fileContent = best_score+"-"+best_level+"-"+scoreTotal+"-"+nbPartiesJouees+"-"+coins+"-"+(int)ameliorations[0][1]+"-"+(int)ameliorations[1][1];
            fileOutputStream.write(fileContent.getBytes());
        } catch (FileNotFoundException e) {
            // exception handling
        } catch (IOException e) {
            // exception handling
        }
    }

    /* Lire le fichier de sauvegarde et remplacer les valeurs */
    public void readSavedData() {

        String content = null;

        try {
            content = new String(Files.readAllBytes(Paths.get(saveFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String stringToSplit = content;
        String[] tempArray;

        /* delimiter */
        String delimiter = "-";

        /* Séparation du contenu du fichier par rapport au delimiter */
        tempArray = stringToSplit.split(delimiter);

        if (tempArray.length == 7) {
            best_score = Integer.parseInt(tempArray[0]);
            best_level = Integer.parseInt(tempArray[1]);
            scoreTotal = Integer.parseInt(tempArray[2]);
            nbPartiesJouees = Integer.parseInt(tempArray[3]);
            coins = Integer.parseInt(tempArray[4]);
            ameliorations[0][1] = Integer.parseInt(tempArray[5]);
            ameliorations[1][1] = Integer.parseInt(tempArray[6]);
        }
        /* Si le fichier de sauvegarde n'est pas au bon format (length != 5) alors nous laissons les valeurs par défaut */

    }

    @Override
    public void start(Stage primaryStage) throws Exception{

        creerContenu(false);
        if (saveFileExists()) {
            readSavedData();
        }

        primaryStage.setTitle("Space Invaders");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
