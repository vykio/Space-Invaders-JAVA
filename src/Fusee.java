import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Fusee extends SpaceInvaders {

    protected double posX, posY;
    protected double sizeX, sizeY;
    protected boolean explosion, detruit;
    protected boolean gagneAvant;
    protected Image img;

    protected double velX;

    public Fusee (double posX, double posY, double sizeX, double sizeY, Image img) {
        this.posX = posX;
        this.posY = posY;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.img = img;
    }

    public void update() {
        posX+=velX;
    }

    public void affiche(GraphicsContext gc) {
        gc.rect(posX, posY, sizeX, sizeY);
        gc.drawImage(img, posX, posY, sizeX, sizeY);
    }

    public void moveRight(double velX) {
        this.velX = velX;
    }
    public void moveLeft(double velX) {
        this.velX = -velX;
    }

    public boolean collision (Tir tir) {


        if (distance(tir.getPosX(), 0, posX+sizeX/2, 0) < (sizeX/2)) {
            if (distance(0, tir.getPosY(), 0, posY) < (sizeY/2-5)) {
                //System.out.println(distance(0, tir.getPosY(), 0, posY));
                return true;
            }
        }
        return false;
    }




}
