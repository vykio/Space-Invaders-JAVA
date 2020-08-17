# Space Invaders
 Projet de ING1 (S6) en programmation orientée objet (JAVA)
 
<p align="center">
  <img src="https://i.imgur.com/puPcEWl.png">
</p>
<br>

# Build le jeu

> :warning: Vous devez avoir ajouté la librairie javafx-sdk à votre projet afin de build correctement

1. Ajoutez les fichiers contenus dans le dossier ```/src``` dans votre dossier projet
2. Configurez les librairies pour ajouter JavaFX ```Java JDK > 10```
3. Lancez

> Si vous faites un build des fichiers sources, l'ajout des librairies javafx sera fera de votre côté.

# Lancer le jeu sur Windows

Utiliser la version déjà build (build avec ```JDK 10```) :
1. Si vous avez ```JDK 10``` : lancer ```out/artifacts/SpaceInvaders_jar/runJDK10.bat```
2. Si vous avez ```JDK 14``` : lancer ```out/artifacts/SpaceInvaders_jar/runJDK14.bat```

> :information_source: Il doit être possible de lancer ```runJDK14.bat``` si vous possédez une version du Jdk supérieure à 10, mais cela n'a **pas été testé**. 

## Informations

Le script ```runJDK14.bat``` importe la librairie ```/javafx-sdk-14.0.1``` pour lancer le .jar, alors que le script ```runJDK10.bat``` lance le .jar nativement car javafx est compris dans le jdk-10.
