package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GameDAO {
    
    private Connection connection;
    
    public GameDAO() throws SQLException {
        this.connection = ConnectionDB.getConnection();
    }
    
    public int guardarLaberinto(Maze laberinto) throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = ConnectionDB.getConnection();
        }
        
        String sql = "INSERT INTO Maze (Name, Num_Col, Num_Row, NumCrocodiles, NumMedKit, " +
                     "DmgCrocodiles, HpMedKit, TimeQuestions, DmgQuestions, EnableHelp) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, laberinto.getName());
            ps.setInt(2, laberinto.getNumCol());
            ps.setInt(3, laberinto.getNumRow());
            ps.setInt(4, laberinto.getNumCrocodiles());
            ps.setInt(5, laberinto.getNumMedKit());
            ps.setInt(6, laberinto.getDmgCrocodiles());
            ps.setInt(7, laberinto.getHpMedKit());
            ps.setInt(8, laberinto.getTimeQuestions());
            ps.setInt(9, laberinto.getDmgQuestions());
            ps.setBoolean(10, laberinto.getEnableHelp());
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating maze failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating maze failed, no ID obtained.");
                }
            }
        }
    }
    
    public Maze getMazeById(int id) throws SQLException {
        Maze maze = null;
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM Maze WHERE ID_Maze=?;");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            maze = new Maze(
                rs.getInt("ID_Maze"),
                rs.getString("Name"),
                rs.getInt("Num_Col"),
                rs.getInt("Num_Row"),
                rs.getInt("NumCrocodiles"),
                rs.getInt("NumMedKit"),
                rs.getInt("DmgCrocodiles"),
                rs.getInt("HpMedKit"),
                rs.getInt("TimeQuestions"),
                rs.getInt("DmgQuestions"),
                rs.getBoolean("EnableHelp")
            );
        }
        return maze;
    }
    
    public Disposition getDispositionById(Maze maze, int dispositionId) throws SQLException {
        if (maze == null) return null;

        PreparedStatement ps = connection.prepareStatement("SELECT * FROM Disposition "
                + "WHERE ID_Maze = ? AND ID_Disposition = ?");
        
        ps.setInt(1, maze.getId());
        ps.setInt(2, dispositionId);
        ResultSet rs = ps.executeQuery();
        
        int[][] cells = new int[maze.getNumRow()][maze.getNumCol()];
        while (rs.next()) {
            int col = rs.getInt("Col_Maze");
            int row = rs.getInt("Row_Maze");
            String cellType = rs.getString("Cell_Type");
            
            switch (cellType) {
                case "Free":
                    cells[row][col] = 0;
                    break;
                case "Block":
                    cells[row][col] = 1;
                    break;
                case "Crocodile":
                    cells[row][col] = 2;
                    break;
                case "Medkit":
                    cells[row][col] = 3;
                    break;
                default:
                    cells[row][col] = -1;
                    break;
            }
        }

        return new Disposition(maze, cells);
    }
    
    public void importQuestions() {
        try {            
            String sql = "SELECT * FROM Questions";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Question q = new Question(
                    rs.getInt("ID_Questions"),
                    rs.getString("Question"),
                    rs.getString("AnswerCorrect"),
                    rs.getString("Answer2"),
                    rs.getString("Answer3"),
                    rs.getString("Answer4")
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Error de SQL al importar preguntas");
            e.printStackTrace();
        }
    }
    
    public void guardarMuros(int idLaberinto, boolean[][] muros) throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = ConnectionDB.getConnection();
        }
        
        // Primero borramos cualquier disposición existente para este laberinto
        try (PreparedStatement psDelete = connection.prepareStatement(
                "DELETE FROM Disposition WHERE ID_Maze = ?")) {
            psDelete.setInt(1, idLaberinto);
            psDelete.executeUpdate();
        }
        
        // Insertamos los nuevos muros
        String sql = "INSERT INTO Disposition (ID_Maze, Col_Maze, Row_Maze, Cell_Type) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement psInsert = connection.prepareStatement(sql)) {
            for (int row = 0; row < muros.length; row++) {
                for (int col = 0; col < muros[row].length; col++) {
                    psInsert.setInt(1, idLaberinto);
                    psInsert.setInt(2, col);
                    psInsert.setInt(3, row);
                    psInsert.setString(4, muros[row][col] ? "Block" : "Free");
                    psInsert.addBatch();
                }
            }
            psInsert.executeBatch();
        }
    }
    
    // Inserta preguntas en la BBDD devuelve si ha sido insertado correctamente
    public boolean guardarPregunta(String enunciado, String respuestaCorrecta,
                                   String respuestaIncorrecta1, String respuestaIncorrecta2, String respuestaIncorrecta3) {
        try  {
        	String query = "INSERT INTO Questions (Question, AnswerCorrect, Answer2, Answer3, Answer4)"
        			+ " VALUES (?, ?, ?, ?, ?)";
        	PreparedStatement st = connection.prepareStatement(query);
            st.setString(1, enunciado);
            st.setString(2, respuestaCorrecta);
            st.setString(3, respuestaIncorrecta1);
            st.setString(4, respuestaIncorrecta2);
            st.setString(5, respuestaIncorrecta3);

            return st.executeUpdate() > 0;

        } catch (SQLException e) {
        	System.out.println("❌ Error SQL al insertar nueva pregunta");
            e.printStackTrace();
        }
        return false;
    }

    public int obtenerUltimoIdDisposicion(int idLaberinto) throws SQLException {
        String sql = "SELECT MAX(ID_Disposition) AS maxId FROM Disposition WHERE ID_Maze = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idLaberinto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("maxId");
            }
        }
        return 0;
    }

    public void insertarDisposicion(Disposition disp, int idDisposicion) throws SQLException {
        String sql = "INSERT INTO Disposition (ID_Maze, ID_Disposition, Col_Maze, Row_Maze, Cell_Type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int[][] cells = disp.getCells();
            for (int row = 0; row < disp.maze.getNumRow(); row++) {
                for (int col = 0; col < disp.maze.getNumCol(); col++) {
                    ps.setInt(1, disp.maze.getId());
                    ps.setInt(2, idDisposicion);
                    ps.setInt(3, col);
                    ps.setInt(4, row);

                    String tipo = switch (cells[row][col]) {
                        case 0 -> "Free";
                        case 1 -> "Block";
                        case 2 -> "Crocodile";
                        case 3 -> "Medkit";
                        default -> "Free";
                    };

                    ps.setString(5, tipo);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    
}