A cause des problèmes de compilation du projet en .JAR sous jdk-14,
Le projet a été compilé sous jdk-10 (qui contient déjà JavaFX).

Vous trouverez néanmoins un script .BAT (pour windows) qui permet de lancer
le .JAR en utilisant la bibliothèque javaFX14 si vous n'avez pas Jdk-10. 

En résumé:
[1] Si vous avez Jdk-10 : vous devez lancer "runJDK10.bat"
[2] Si vous avez Jdk-14 : vous devez lancer "runJDK14.bat"

Remarque importante:
Il doit être possible de lancer "runJDK14.bat" si vous possédez une version du 
Jdk supérieure à 10; Mais cela n'a pas été testé. 

Si vous avez un problème de lancement, veuillez me contacter sur keybase @ vykio


Remarque (bis):
Le projet a été configuré sous jdk-10. Si vous voulez lancer le projet, 
	il faudra peut-être configurer le projet pour se lancer sous jdk14
	et donc importer votre librairie javafx.