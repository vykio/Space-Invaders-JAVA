import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Tir extends SpaceInvaders {

    private double posX, posY;
    final private int speed = 10;
    final private double sizeX = 3;
    final private double sizeY = 13;
    private int direction = -1;
    protected boolean isByEnemy = false;
    protected double vitesse = 0.6;
    protected double vitesse_enemy = 0.6;

    public Tir (double posX, double posY) {
        this.posX = posX-sizeX/2;
        this.posY = posY;
    }

    public Tir (double posX, double posY, int direction, boolean isByEnemy) {
        this(posX, posY);
        this.direction=direction;
        this.isByEnemy = isByEnemy;
    }


    public void update() {
        if (isByEnemy) {
            this.posY += direction*vitesse_enemy;
        } else {
            this.posY += direction*vitesse;
        }

    }

    public void affiche(GraphicsContext gc) {
        if (isByEnemy) {
            gc.setFill(Color.ORANGERED);
        } else {
            gc.setFill(Color.LIGHTBLUE);
        }

        gc.fillOval(posX, posY, sizeX, sizeY);
    }

    public boolean collision(Alien alien) {
        //System.out.println(alien.sizeX);
        //System.out.println(distance(posX, posY, alien.posX, alien.posY));
        if (!isByEnemy) {
            if (distance(posX, 0, alien.posX+alien.sizeX/2, 0) < (alien.sizeX/2)) {
                if (distance(0, posY, 0, alien.posY) < (alien.sizeY)) {
                    return true;
                }
            }
            return false;
        } return false;

    }

    public double getPosX() {
        return posX;
    }
    public double getPosY() {
        return posY;
    }
    public double getSizeX() {
        return sizeX;
    }
    public double getSizeY() {
        return sizeY;
    }

}
