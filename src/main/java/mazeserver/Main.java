/**
 * Main.java (Maze Server)
 *
 * Copyright 2016 Finn Bear.  All rights reserved.
 */

package mazeserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;


public class Main extends HttpServlet {

    // For now, all mazes are the same size.
    private static final int _MAZE_HEIGHT = 20;
    private static final int _MAZE_WIDTH = 20;

    // For now, all games are kept in RAM.
    private Map<Long, Game> _gameHash;
    private Map<Long, Game> _sessionHash;


    /**
     * GET /12345
     *
     * This method returns finds or creates a game instance which matches the
     * specified random seed (e.g. 12345), and then returns the maze and the
     * session ID of a player game object in said game.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        assert req != null;
        assert resp != null;
        assert _gameHash != null;
        assert _sessionHash != null;
        PrintWriter out = resp.getWriter();

        try {
            String p = req.getServletPath();
            assert p != null;
            boolean debugging = p.equals("/");

            // Parse the seed and then find or create a maze with said seed.
            long seed;
            if (debugging) {
                seed = new Date().getTime();
            } else {
                String[] a = p.split("/");
                String arg = a.length < 2 ? null : a[1];
                seed = parseId(arg);
            }
            Long key = new Long(seed);
            Game game = _gameHash.get(key);
            if (game == null) {
                game = new Game(seed, _MAZE_HEIGHT, _MAZE_WIDTH);
                _gameHash.put(key, game);
            }

            // Create a unique session ID.
            assert game != null;
            Game g;
            long sessionId;
            do {
                sessionId = game.getMaze().randomInt(0xFFFFFF);
                g = _sessionHash.get(sessionId);
            } while (g != null);

            // Create a new player.
            game.createPlayer(sessionId);
            _sessionHash.put(new Long(sessionId), game);

            // Return the maze.
            if (debugging) {
                printDebug(game.getMaze(), out);
            } else {
                printGame(game.getMaze(), out);
            }

            resp.setStatus(HttpStatus.OK_200);
        } catch (Throwable ex) {
            out.println(ex.toString());
            resp.setStatus(HttpStatus.OK_200);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        assert req != null;
        assert resp != null;
        PrintWriter out = resp.getWriter();
        try {
            String p = req.getServletPath();
            assert p != null;
            String[] a = p.split("/");
            String arg = a.length < 2 ? null : a[1];
            assert arg != null;
            long sessionId = parseId(arg);
            Game game = _sessionHash.get(new Long(sessionId));
            if (game == null) {
                throw new NoSuchElementException("cannot find session " + sessionId);
            }

            String line;
            BufferedReader input = req.getReader();
            while ((line = input.readLine()) != null) {
                out.print(parseJson(line));
            }

            resp.setStatus(HttpStatus.OK_200);
        } catch (NoSuchElementException ex) {
            resp.setStatus(HttpStatus.NOT_FOUND_404);
        } catch (Throwable ex) {
            out.println(ex.toString());
            resp.setStatus(HttpStatus.OK_200);
        }
    }

    @Override
    public void init() {
        _gameHash = new HashMap();
        _sessionHash = new HashMap();
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        ServletContextHandler handler = new ServletContextHandler(server, "/saturnbackend");
        handler.addServlet(Main.class, "/");
        server.start();
    }

    private static long parseId(String arg) {
        long id;
        try {
            id = Long.parseLong(arg);
        } catch (NumberFormatException ex) {
            id = 13579111315L;
        }
        return id;
    }

    private static String parseJson(String arg) {
        String json = null;
        if (arg != null) {
            int i = arg.indexOf("{");
            int j = arg.lastIndexOf("}");
            if (i >= 0 && j >= 0 && i < j) {
                json = arg.substring(i, j + 1);
            } else {
                json = "";
            }
        }
        return json;
    }

    private void printDebug(Maze maze, PrintWriter out) {

            out.println("<html>\n<title>maze</title>\n<body>");

            StringBuffer svg = new StringBuffer();
            final int scale = 15;
            final int unscaledPadding = Math.max(_MAZE_HEIGHT / 2, _MAZE_WIDTH / 2);
            final int scaledPadding = scale * unscaledPadding;
            final int scaledHeight = scale * _MAZE_HEIGHT;
            final int scaledWidth = scale * _MAZE_WIDTH;

            final int xBounds = scaledPadding / 2;
            final int yBounds = xBounds;
            final int xCenter = (scaledPadding + scaledWidth) / 2;
            final int yCenter = (scaledPadding + scaledHeight) / 2;
            svg.append(MessageFormat.format( "<svg height=\"{0}\" width=\"{1}\">\n",
                scaledPadding + scaledHeight, scaledPadding + scaledWidth ));

            // Bounding box and center (x, y).
            svg.append(MessageFormat.format("  <rect x=\"{0}\" y=\"{1}\" height=\"{2}\" width=\"{3}\" style=\"stroke-width:1;stroke:rgb(0,0,255);fill:rgb(255,255,255);stroke-opacity:0.2;\" />\n", xBounds, yBounds, scaledHeight, scaledWidth));
            svg.append(MessageFormat.format( "  <line x1=\"{0}\" y1=\"{1}\" x2=\"{2}\" y2=\"{3}\" style=\"stroke:rgb(0,0,255);stroke-opacity:0.2;stroke-width:1\" />\n", xCenter, yBounds, xCenter, yBounds + scaledHeight));
            svg.append(MessageFormat.format( "  <line x1=\"{0}\" y1=\"{1}\" x2=\"{2}\" y2=\"{3}\" style=\"stroke:rgb(0,0,255);stroke-opacity:0.2;stroke-width:1\" />\n", xBounds, yCenter, xBounds + scaledWidth, yCenter));

            for (Maze.Line line : maze.getContent().getLines()) {
                svg.append(MessageFormat.format( "  <line x1=\"{0}\" y1=\"{1}\" x2=\"{2}\" y2=\"{3}\" style=\"stroke:rgb(255,0,0);stroke-width:2\" />\n",
                    xCenter + scale * line.getStart().getX(),
                    yCenter + scale * line.getStart().getY(),
                    xCenter + scale * line.getEnd().getX(),
                    yCenter + scale * line.getEnd().getY() ));
            }
            svg.append("</svg>\n");
            out.println(svg.toString());

            out.println("\n<a href=\"" + maze.getId() + "\">json</a>\n");
            out.println("<form action=\"" + maze.getId() + "\" enctype=\"multipart/form-data\" method=\"POST\" name=\"play\">");
            out.println("<input name=\"json\" type=\"file\" />");
            out.println("<input type=\"submit\" value=\"Play\" />");
            out.println("</form>");
            out.println("</body>\n</html>");
    }

    private void printGame(Maze maze, PrintWriter out) {
        Gson gson = new GsonBuilder().create();
        gson.toJson(maze.getContent(), out);
    }

}
