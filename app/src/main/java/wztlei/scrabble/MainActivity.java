package wztlei.scrabble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {


    ArrayList<String> boardStrings;
    HashMap<Integer, Square> boardButtonIDs;
    int lastSquareClickedID;
    ScrabbleEngine scrabbleEngine;
    Square[][] scrabbleBoard = null;
    String oldScrabbleBoard = "";
    final String savedScrabbleKey = "savedScrabbleBoard";


    /**
     * @return  an unordered map of Strings containing all the words in the
     *          scrabble dictionary. The key is type String since it is stores
     *          the word. The mapped value is type integer since it stores if
     *          the word is worth a bonus multiplier.
     */
    public HashMap <String, Integer> readWordData () {

        // Declare hash map to store all of the words in the scrabble dictionary
        HashMap <String, Integer> wordHashMap = new HashMap<> ();
        TextFileNames textFileNames = new TextFileNames();
        String wordsFileName = textFileNames.wordsFileName;

        Scanner wordDataFile = null;

        try {
            InputStream inputStream = getAssets().open(wordsFileName);
            wordDataFile = new Scanner (inputStream);
        } catch (FileNotFoundException ex) {
            System.out.println("Could not open " + wordsFileName);
        } catch (IOException ex) {
            System.out.println("IOException due to " + wordsFileName);
        }

        // If file is opened
        if (wordDataFile != null) {
            while(wordDataFile.hasNextLine()) {
                // IMPORTANT: The words must all be in uppercase.
                String word = wordDataFile.nextLine();

                // Insert the word into the unordered map
                wordHashMap.put(word, 1);
            }
        }

        return wordHashMap;
    }

    /**
     * @return  an ArrayList of Tiles with each tile object containing the
     *          right data.
     */
    public Tile[] readTileData () {

        // Declare array to store all the Tiles
        Tile[] tileArray = new Tile[27];
        TextFileNames textFileNames = new TextFileNames();
        String tilesFileName = textFileNames.tilesFileName;

        Scanner tileDataFile = null;

        try {
            InputStream inputStream = getAssets().open(tilesFileName);
            tileDataFile = new Scanner (inputStream);
        } catch (FileNotFoundException ex) {
            System.out.println("Could not open " + tilesFileName);
        } catch (IOException ex) {
            System.out.println("IOException due to " + tilesFileName);
        }

        // Ensure data file is open
        if (tileDataFile != null) {
            // Loop through all 27 possible tiles and add them to the vector
            for (int i = 0; i < 27; i++) {
                Tile tile = new Tile();
                tile.letter = tileDataFile.next().charAt(0);
                tile.points = Integer.parseInt(tileDataFile.next());
                tile.total = Integer.parseInt(tileDataFile.next());
                tileArray[i] = tile;
            }
        }

        return tileArray;
    }

    /**
     * @return  a SquareGrid containing the data for each square on the board.
     *          Key for the text file's characters:
     *              W = Triple Word Score
     *              w = Double Word Score
     *              L = Triple Letter Score
     *              l = Double Letter Score
     *              . = Regular Square
     *              * = Square is out of bounds
     */
    public Square [][] readBoardData () {

        // Declare board array
        Square[][] board = new Square[17][17];
        TextFileNames textFileNames = new TextFileNames();
        String boardFileName = textFileNames.boardFileName;

        Scanner boardDataFile = null;

        try {
            InputStream inputStream = getAssets().open(boardFileName);
            boardDataFile = new Scanner (inputStream);
        } catch (FileNotFoundException ex) {
            System.out.println("Could not open " + boardFileName);
        } catch (IOException ex) {
            System.out.println("IOException due to " + boardFileName);
        }

        // Ensure file is open
        if (boardDataFile != null) {
            int rowNum = 0;

            // Get all the rows in the board
            // The rows of x's around the actual board are to ensure that
            // tiles are not added outside the board
            while (boardDataFile.hasNextLine()) {

                // Read a line from the text file
                String line = boardDataFile.nextLine();

                // Declare an Array of Squares to store the data for each row
                Square[] row = new Square[17];

                // Go through all the characters in each line
                for (int i = 0; i < line.length(); i++) {
                    Square sqr = new Square();
                    sqr.letter = '.';
                    sqr.row = rowNum;
                    sqr.col = i;

                    // Assign the Square type to sqr
                    switch (line.charAt(i)) {
                        case 'W': sqr.type = SquareType.TRIPLE_WORD;   break;
                        case 'w': sqr.type = SquareType.DOUBLE_WORD;   break;
                        case 'L': sqr.type = SquareType.TRIPLE_LETTER; break;
                        case 'l': sqr.type = SquareType.DOUBLE_LETTER; break;
                        case '.': sqr.type = SquareType.REGULAR;       break;
                        case 'x': sqr.type = SquareType.OUTSIDE;       break;
                    }

                    // Assign the downCrossCheck vector to sqr
                    switch (line.charAt(i)) {
                        case 'x':
                            sqr.downCrossCheck = new boolean[26];
                            Arrays.fill(sqr.downCrossCheck, false);
                            sqr.letter = '.';
                            break;
                        default:
                            sqr.downCrossCheck = new boolean[26];
                            Arrays.fill(sqr.downCrossCheck, true);
                            sqr.letter = '.';
                            break;
                    }

                    row[i] = sqr;
                }

                board[rowNum] = row;
                rowNum++;
            }

        }

        scrabbleEngine.updateDownCrossChecks(board);
        scrabbleEngine.updateMinAcrossWordLength(board);

        return board;
    }

    /**
     * Fills the board with letters which are read from a text file.
     *
     * @param   board   a square grid containing the data for the state of the
     *                  game.
     */
    public void readTestGameData (Square[][] board) {
        // Open file containing the data
        Scanner gameDataFile = null;
        TextFileNames textFileNames = new TextFileNames();
        String gameFileName = textFileNames.gameFileName;

        try {
            InputStream inputStream = getAssets().open(gameFileName);
            gameDataFile = new Scanner (inputStream);
        } catch (FileNotFoundException ex) {
            System.out.println("Could not open " + gameFileName);
        } catch (IOException ex) {
            System.out.println("IOException due to " + gameFileName);
        }

        // Ensure file is open
        if (gameDataFile != null) {
            // Go through all the rows
            for (int rowNum = 0; rowNum < 15; rowNum++) {
                // Get each row as input
                String input = gameDataFile.next();

                // Go through all the rows
                for (int colNum = 0; colNum < 15; colNum++) {
                    // row+1 and row+1 are used since the top row and column
                    // (row 0 and column 0) of board are used to mark outside
                    // squares
                    // Fill in the tiles on the board
                    board[rowNum + 1][colNum + 1].letter = input.charAt(colNum);
                }
            }
        }

        scrabbleEngine.updateDownCrossChecks(board);
        scrabbleEngine.updateMinAcrossWordLength(board);
    }

    /**
     * Changes the height and width of each button in the grid of Scrabble squares
     * so that they are squares and together they completely fill the device screen
     */
    protected void setButtonDimensions() {

        // Get the width of the device in pixels
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;

        // Get the table by ID
        TableLayout tableLayout = findViewById(R.id.table_scrabble_board);

        // Go through every button in the displayed Scrabble board
        for (int tableRowNum = 0; tableRowNum < 15; tableRowNum++) {
            TableRow tableRow = (TableRow)tableLayout.getChildAt(tableRowNum);

            for (int tableColNum = 0; tableColNum < tableRow.getChildCount(); tableColNum++) {
                // Change the height and width of the button to make it a square
                View view = tableRow.getChildAt(tableColNum);
                Button square = findViewById(view.getId());
                square.getLayoutParams().height = displayWidth / 15;
                square.getLayoutParams().width = displayWidth / 15;
            }
        }
    }

    /**
     * Function is called to update the state of scrabbleBoard whenever.
     */
    protected void setButtonTexts () {

        // Get the table by ID
        TableLayout tableLayout = findViewById(R.id.table_scrabble_board);

        // Go through all the buttons
        for (int tableRowNum = 0; tableRowNum < 15; tableRowNum++) {
            TableRow tableRow = (TableRow) tableLayout.getChildAt(tableRowNum);

            for (int tableColNum = 0; tableColNum < 15; tableColNum++) {
                Button square = (Button) tableRow.getChildAt(tableColNum);
                if (scrabbleBoard[tableRowNum+1][tableColNum+1].letter == '.') {
                    square.setText("");
                }
                else {
                    String str = "";
                    str += scrabbleBoard[tableRowNum+1][tableColNum+1].letter;
                    square.setText(str);
                }
            }
        }
    }


    protected void readBoardStrings() {
        TextFileNames textFileNames = new TextFileNames();
        String boardFileName = textFileNames.boardFileName;

        Scanner boardDataFile = null;

        try {
            InputStream inputStream = getAssets().open(boardFileName);
            boardDataFile = new Scanner (inputStream);

        } catch (FileNotFoundException ex) {
            System.out.println("Could not open " + boardFileName);
        } catch (IOException ex) {
            System.out.println("IOException due to " + boardFileName);
        }

        boardStrings = new ArrayList<>();

        // Ensure file is open
        if (boardDataFile != null) {

            // Get all the lines in the board text file
            while (boardDataFile.hasNextLine()) {

                // Read a line from the text file
                String line = boardDataFile.nextLine();
                boardStrings.add(line);
            }
        }
    }

    /**
     * Stores all the button IDs in the Scrabble board.
     */
    @SuppressLint("UseSparseArrays")
    protected void storeButtonIDs() {

        // Get the table by ID
        TableLayout tableLayout = findViewById(R.id.table_scrabble_board);

        // Initialize the array to store all the IDs
        boardButtonIDs = new HashMap<>();

        // Go through every button in the Scrabble board
        // getChildCount() is not used since there are items
        // below the grid that should not be changed
        for (int tableRowNum = 0; tableRowNum < 15; tableRowNum++) {

            TableRow tableRow = (TableRow)tableLayout.getChildAt(tableRowNum);

            for (int tableColNum = 0; tableColNum < tableRow.getChildCount(); tableColNum++) {

                View view = tableRow.getChildAt(tableColNum);
                Square sqr = new Square();
                sqr.row = tableRowNum + 1;
                sqr.col = tableColNum + 1;
                boardButtonIDs.put(view.getId(), sqr);
            }
        }
    }

    /**
     * Sets the colours of the buttons so that they reflect the colours of a Scrabble board.
     */
    protected void setButtonColors() {

        // Get the table by ID
        TableLayout tableLayout = findViewById(R.id.table_scrabble_board);

        // Initialize the array to store all the IDs

        // Go through every button in the Scrabble board and the tiles below
        for (int tableRowNum = 0; tableRowNum < 15; tableRowNum++) {

            TableRow tableRow = (TableRow)tableLayout.getChildAt(tableRowNum);

            for (int tableColNum = 0; tableColNum < tableRow.getChildCount(); tableColNum++) {

                View view = tableRow.getChildAt(tableColNum);
                Button square = findViewById(view.getId());
                char squareChar = boardStrings.get(tableRowNum+1).charAt(tableColNum+1);

                // Include an exception if the square already has a tile on it
                if (square.getText().length() > 0) {
                    squareChar = 't';
                }

                // Change the background to the proper drawable resource
                switch (squareChar) {
                    case 'W': square.setBackgroundResource(R.drawable.triple_word_square);   break;
                    case 'w': square.setBackgroundResource(R.drawable.double_word_square);   break;
                    case 'L': square.setBackgroundResource(R.drawable.triple_letter_square); break;
                    case 'l': square.setBackgroundResource(R.drawable.double_letter_square); break;
                    case '.': square.setBackgroundResource(R.drawable.regular_square);       break;
                    case 't': square.setBackgroundResource(R.drawable.tile_square);          break;
                }
            }
        }
    }



    /**
     * Function is called to hide the keyboard
     *
     * @param activity the activity where the keyboard needs to be hidden
     */
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService
                                                                (Activity.INPUT_METHOD_SERVICE);

        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();

        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }

        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);

        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();

        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }

        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }



    /**
     * Changes the border of the button to white
     *
     * @param buttonID the ID of the button whose border needs to be changed to white
     */
    protected void drawButtonWhiteBorder(int buttonID) {

        Button boardSquare = findViewById(buttonID);

        // Get the row and col number of the button pressed
        int rowNum = boardButtonIDs.get(buttonID).row;
        int colNum = boardButtonIDs.get(buttonID).col;
        char squareChar = boardStrings.get(rowNum).charAt(colNum);

        // Use the tile background if the button has a tile on it
        if (boardSquare.getText().length() > 0 && !boardSquare.getText().equals(" ")) {
            boardSquare.setBackgroundResource(R.drawable.tile_square);
            return;
        }

        // Change the background of the current button to the proper drawable resource
        // This will cause the button to have a black border
        switch (squareChar) {
            case 'W':
                boardSquare.setBackgroundResource(R.drawable.triple_word_square);
                break;
            case 'w':
                boardSquare.setBackgroundResource(R.drawable.double_word_square);
                break;
            case 'L':
                boardSquare.setBackgroundResource(R.drawable.triple_letter_square);
                break;
            case 'l':
                boardSquare.setBackgroundResource(R.drawable.double_letter_square);
                break;
            case '.':
                boardSquare.setBackgroundResource(R.drawable.regular_square);
                break;
            case 't':
                boardSquare.setBackgroundResource(R.drawable.tile_square);
                break;
        }
    }

    /**
     * Changes the border of the button to black
     *
     * @param buttonID the ID of the button whose border needs to be changed to black
     */
    protected void drawButtonBlackBorder(int buttonID) {

        Button boardSquare = findViewById(buttonID);

        // Get the row and col number of the button pressed
        int rowNum = boardButtonIDs.get(buttonID).row;
        int colNum = boardButtonIDs.get(buttonID).col;
        char squareChar = boardStrings.get(rowNum).charAt(colNum);

        // Use the tile background if the button has a tile on it
        if (boardSquare.getText().length() > 0 && !boardSquare.getText().equals(" ")) {
            boardSquare.setBackgroundResource(R.drawable.tile_square_pressed);
            return;
        }

        // Change the background of the current button to the proper drawable resource
        // This will cause the button to have a black border
        switch (squareChar) {
            case 'W':
                boardSquare.setBackgroundResource(R.drawable.triple_word_square_pressed);
                break;
            case 'w':
                boardSquare.setBackgroundResource(R.drawable.double_word_square_pressed);
                break;
            case 'L':
                boardSquare.setBackgroundResource(R.drawable.triple_letter_square_pressed);
                break;
            case 'l':
                boardSquare.setBackgroundResource(R.drawable.double_letter_square_pressed);
                break;
            case '.':
                boardSquare.setBackgroundResource(R.drawable.regular_square_pressed);
                break;
            case 't':
                boardSquare.setBackgroundResource(R.drawable.tile_square_pressed);
                break;
        }
    }

    /**
     * Function is called when the user clicks "Enter" to change a tile on the board
     *
     * @param view the ID of the button whose border needs to be changed to white
     */
    public void onClickEnterBoardTile(View view) {

        if (lastSquareClickedID != 0) {
            // Change the text displayed on the tile on the board
            Button boardSquare = findViewById(lastSquareClickedID);
            EditText boardEditText = findViewById(R.id.edit_text_board);
            String inputtedTileLetter = boardEditText.getText().toString();

            // To erase a tile from the board
            if (inputtedTileLetter.length() == 0) {
                boardSquare.setText(inputtedTileLetter);
                updateStoredScrabbleBoard();
            }
            // To add a tile to the board
            else if (inputtedTileLetter.length() == 1 &&
                    Character.isLetter(inputtedTileLetter.charAt(0))) {
                boardSquare.setText(inputtedTileLetter);
                updateStoredScrabbleBoard();
            }
            // Invalid input by user to change a tile on the board
            else {

                // Create an Alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Invalid Input")
                        .setMessage("Enter an uppercase letter to add a regular tile or " +
                                "enter a lowercase letter to add a blank tile. \n\n" +
                                "Leave the text field empty to remove the tile.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {}
                        });

                // Get the AlertDialog from create()
                AlertDialog dialog = builder.create();
                dialog.show();
            }

            // Hide the keyboard
            hideKeyboard(this);

            // Change the background of the button
            drawButtonBlackBorder(lastSquareClickedID);
        }
    }

    /**
     * Function is called when a button in the Scrabble board is pressed
     *
     * @param view the view of the button pressed
     */
    public void onClickBoardButton (View view) {

        int currButtonID = view.getId();
        Button boardSquare = findViewById(currButtonID);

        showKeyboard(this);

        // Set the text of the input text box (to change a tile on the board)
        // to the text currently on the button
        EditText boardEditText = findViewById(R.id.edit_text_board);
        boardEditText.setText(boardSquare.getText());
        boardEditText.requestFocus();
        boardEditText.selectAll();

        // Change the borders of the last button pressed
        if (lastSquareClickedID != 0) {
            drawButtonWhiteBorder(lastSquareClickedID);
        }

        // Change the borders of the current button pressed
        drawButtonBlackBorder(view.getId());

        // Update the variable storing the ID of the last button pressed
        lastSquareClickedID = view.getId();
    }

    /**
     * Function is called when the user clicks "Enter" to change the tiles in the rack
     *
     * @param view the ID of the clicked button
     */
    public void onClickEnterRackTiles(View view) {
        EditText rackEditText = findViewById(R.id.edit_text_rack);
        String rackStr = rackEditText.getText().toString();

        // Check to see if the inputted rack string is valid
        if (!scrabbleEngine.rackStringIsValid(rackStr) || rackStr.length() == 0) {
            displayRackError();
        }
        // If there are too many or too little tiles in the rack,
        // display a warning but allow the user to continue
        else if (rackStr.length() != 7) {
            displayRackWarning();
        }

        hideKeyboard(this);
        view.clearFocus();
    }

    /**
     * Function is called to update the state of scrabbleBoard whenever.
     */
    protected void updateStoredScrabbleBoard () {
        // Get the table by ID
        TableLayout tableLayout = findViewById(R.id.table_scrabble_board);

        // Go through every button in the displayed Scrabble board
        for (int tableRowNum = 0; tableRowNum < 15; tableRowNum++) {
            TableRow tableRow = (TableRow)tableLayout.getChildAt(tableRowNum);

            for (int tableColNum = 0; tableColNum < tableRow.getChildCount(); tableColNum++) {

                Button square = (Button) tableRow.getChildAt(tableColNum);

                if (square.getText().length() == 0) {
                    scrabbleBoard[tableRowNum+1][tableColNum+1].letter = '.';
                }
                else {
                    scrabbleBoard[tableRowNum+1][tableColNum+1].letter = square.getText().charAt(0);
                }
            }
        }

        scrabbleEngine.updateDownCrossChecks(scrabbleBoard);
        scrabbleEngine.updateMinAcrossWordLength(scrabbleBoard);

        oldScrabbleBoard = scrabbleEngine.boardTilesToString(scrabbleBoard);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        String savedBoardString = scrabbleEngine.boardTilesToString(scrabbleBoard);

        savedInstanceState.putString(savedScrabbleKey, savedBoardString);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        readBoardStrings();
        storeButtonIDs();
        setButtonDimensions();

        scrabbleEngine = new ScrabbleEngine(readWordData(), readTileData());
        scrabbleBoard = readBoardData();

        // Create an warning Alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Welcome!!")
                .setMessage("Do you want to load the example board?")
                .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                    // Load the example board
                    public void onClick(DialogInterface dialog, int id) {
                        readTestGameData(scrabbleBoard);
                        setButtonTexts();
                        setButtonColors();
                        oldScrabbleBoard = scrabbleEngine.boardTilesToString(scrabbleBoard);
                    }
                })
                .setPositiveButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        // Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();

        if (savedInstanceState != null) {

            String savedBoardString = savedInstanceState.getString(savedScrabbleKey);
            scrabbleEngine.fillBoardWithString(scrabbleBoard, savedBoardString);
            setButtonTexts();
        }

        setButtonColors();

        // Change the SelectAllOnFocus for the EditText field
        // since the property is not working in the XML
        EditText rackEditText = findViewById(R.id.edit_text_rack);
        rackEditText.setSelectAllOnFocus(true);

        lastSquareClickedID = 0;
    }

    protected void displayRackError() {
        // Create an error Alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Invalid Input")
                .setMessage("Enter uppercase letters for regular tiles " +
                        "and asterisks (*) for blank tiles.")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        // Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void displayRackWarning() {
        // Create an warning Alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning")
                .setMessage("For a standard Scrabble game, each player should have seven tiles.")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        // Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Function is called when the user clicks "Find Best Move"
     *
     * @param view the ID of the clicked button
     */
    public void onClickFindBestMove(View view) {
        EditText rackEditText = findViewById(R.id.edit_text_rack);
        String rackStr = rackEditText.getText().toString();

        // Check to see if the inputted rack string is valid
        if (!scrabbleEngine.rackStringIsValid(rackStr) || rackStr.length() == 0) {
            displayRackError();
            return;
        }

        // Find the best move
        int[] rack = scrabbleEngine.fillRack(rackStr);
        ScrabbleMove bestMove = scrabbleEngine.findBestMove(scrabbleBoard, rack);
        scrabbleEngine.addMoveToBoard(scrabbleBoard, bestMove);

        // Update the display
        setButtonTexts();
        setButtonColors();

        // Create an Alert dialog to tell the user information about the best move
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String bestMoveMessage = "Points: " + bestMove.points + "\n";

        // Only output the specifics of the move if it exists
        if (bestMove.size() > 0) {
            bestMoveMessage += "Tiles Placed: ";

            for (int i = 0; i < bestMove.size(); i++) {
                bestMoveMessage += bestMove.get(i).letter + " ";
            }
        }

        builder.setTitle("Best Move")
                .setMessage(bestMoveMessage)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        // Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();


        // If there are too many or too little tiles in the rack,
        // display a warning but allow the user to continue
        if (rackStr.length() != 7) {
            displayRackWarning();
        }
    }

    public void onClickEraseMove(View view) {
        if (scrabbleBoard != null && oldScrabbleBoard.length() > 0) {
            scrabbleEngine.fillBoardWithString(scrabbleBoard, oldScrabbleBoard);
            oldScrabbleBoard = scrabbleEngine.boardTilesToString(scrabbleBoard);

            // Update the display
            setButtonTexts();
            setButtonColors();
        }
    }
}
