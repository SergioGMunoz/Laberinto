package test;

import java.sql.SQLException;

import controller.GameController;
import model.ConnectionDB;
import view.PantallaInicio;

public class GameQuizTest {
    public static void main(String[] args) {
    	
        if (ConnectionDB.connect("JavaLaberinto", "Java12345", "localhost", "3306", "Laberinto") != null) {
            //Crear partida de prueba ID  1 dispo 1
        	try {
        		PantallaInicio.setUser("Javi");
				GameController gc = new GameController(1,1);
			} catch (SQLException e) {
				System.out.println("Error de SQL al crear GameController");
				e.printStackTrace();
			}
        }
      
    }
}

