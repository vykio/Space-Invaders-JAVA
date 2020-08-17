import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Alien extends Fusee {

    static protected double vitesse=0.075, direction=1;
    protected int velX;

    public Alien (double posX, double posY, double sizeX, double sizeY, Image img){
        super(posX, posY, sizeX, sizeY, img);
    }

    public void update() {
        posX+=direction*vitesse;
    }

    public void affiche(GraphicsContext gc) {
        gc.drawImage(img, posX, posY, sizeX, sizeY);
    }

}
