package com.example.lifegame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private int lightCoral, lightRed, darkGray, gray, white, darkenedWhite;
    private Drawable playIcon, pauseIcon, arrowDownIcon, arrowUpIcon;

    private LifeMatrix matrix;
    private int firstTouchDistance = -1;
    private int cellSize, tempCellSize;
    private int screenWidth, screenHeight;
    private int timelapseStepDelay = 50;
    private float dp;
    private Point currentCell;
    private ImageView field;
    private SeekBar widthSeekBar;
    private SeekBar heightSeekBar;
    private EditText widthTextNumber, heightTextNumber, speedTextNumber;
    private TableLayout settingsLayout;
    private Bitmap bitmap;
    private Canvas canvas;
    private TlThread timelapse;

    public static boolean isEraserEnabled = false, isTorus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playIcon = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_media_play, null);
        pauseIcon = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_media_pause, null);
        arrowDownIcon = ResourcesCompat.getDrawable(getResources(), android.R.drawable.arrow_down_float, null);
        arrowUpIcon = ResourcesCompat.getDrawable(getResources(), android.R.drawable.arrow_up_float, null);

        lightCoral = ResourcesCompat.getColor(getResources(), R.color.lightCoral, null);
        lightRed = ResourcesCompat.getColor(getResources(), R.color.lightRed, null);
        darkGray = ResourcesCompat.getColor(getResources(), R.color.darkGray, null);
        gray = ResourcesCompat.getColor(getResources(), R.color.gray, null);
        white = Color.WHITE;
        darkenedWhite = ResourcesCompat.getColor(getResources(), R.color.darkenedWhite, null);

        setContentView(R.layout.activity_main);
        field = findViewById(R.id.Field);
        field.setOnTouchListener(this);
        ConstraintLayout mainConstraintLayout = findViewById(R.id.MainConstraintLayout);
        mainConstraintLayout.setOnTouchListener(this);
        Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        currentCell = new Point();
        screenWidth = screenSize.x;
        screenHeight = screenSize.y;
        dp = getResources().getDisplayMetrics().density;
        cellSize = (int)(20 * dp);
        tempCellSize = cellSize;
        widthSeekBar = findViewById(R.id.WidthSeekBar);
        widthSeekBar.setMax(screenWidth / cellSize);
        widthSeekBar.setProgress(10);
        SeekBar speedSeekBar = findViewById(R.id.SpeedSeekBar);
        speedSeekBar.setProgress(20);
        heightSeekBar = findViewById(R.id.HeightSeekBar);
        heightSeekBar.setMax(screenHeight / cellSize);
        heightSeekBar.setProgress(15);
        widthTextNumber = findViewById(R.id.WidthTextNumber);
        widthTextNumber.setText(String.valueOf(widthSeekBar.getProgress()));
        heightTextNumber = findViewById(R.id.HeightTextNumber);
        heightTextNumber.setText(String.valueOf(heightSeekBar.getProgress()));
        speedTextNumber = findViewById(R.id.SpeedTextNumber);
        speedTextNumber.setText(String.valueOf(20));
        settingsLayout = findViewById(R.id.SettingsLayout);

        createField(cellSize, widthSeekBar.getProgress(), heightSeekBar.getProgress());
        matrix = new LifeMatrix(widthSeekBar.getProgress(), heightSeekBar.getProgress());
        timelapse = new TlThread("timelapse");

        SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isNewSizeSuitable(cellSize)) {
                    createField(cellSize, widthSeekBar.getProgress(), heightSeekBar.getProgress());
                }
                if (seekBar.getId() == R.id.WidthSeekBar) {
                    widthTextNumber.setText(String.valueOf(seekBar.getProgress()));
                } else {
                    heightTextNumber.setText(String.valueOf(seekBar.getProgress()));
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) { }
        };
        widthSeekBar.setOnSeekBarChangeListener(seekBarListener);
        heightSeekBar.setOnSeekBarChangeListener(seekBarListener);

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                timelapseStepDelay = (int)(1000.0 / seekBar.getProgress());
                speedTextNumber.setText(String.valueOf(seekBar.getProgress()));
            }

            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        speedTextNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                setSeekBarValueFromEditText(text, speedSeekBar, speedTextNumber);
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        });

        widthTextNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                setSeekBarValueFromEditText(text, widthSeekBar, widthTextNumber);
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        });

        heightTextNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                setSeekBarValueFromEditText(text, heightSeekBar, heightTextNumber);
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        });
    }

    private void setSeekBarValueFromEditText (String text, SeekBar seekBar, EditText textNumber) {
        int value = seekBar.getProgress();
        int length;

        if (text.length() > 0) {
            value = Integer.parseInt(text);
        }
        if (value < 1) {
            value = 1;
        }
        if (value > seekBar.getMax()) {
            value = seekBar.getMax();
        }
        if (value < 1 || value > seekBar.getMax()) {
            textNumber.setText(String.valueOf(value));
        }
        length = textNumber.getText().length();
        textNumber.setSelection(length);
        seekBar.setProgress(value);
    }

    private boolean isNewSizeSuitable(int cellSize) {
        return widthSeekBar.getProgress() * cellSize <= screenWidth && heightSeekBar.getProgress() * cellSize <= screenHeight;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();

        if (!timelapse.isAlive()) {
            switch (pointerCount) {
                case 1:
                    settingsLayout.setVisibility(View.INVISIBLE);
                    if (view.getId() == R.id.Field) {
                        Point fingerLocation = new Point((int)motionEvent.getX(0), (int)motionEvent.getY(0));
                        Point newCell = getCurrentCell(fingerLocation);
                        if (newCell != currentCell) {
                            currentCell = newCell;
                            if (newCell.x >= 0 && newCell.y >= 0 && newCell.x  < matrix.getWidth() && newCell.y < matrix.getHeight()) {
                                matrix.setCell(newCell.x, newCell.y, !isEraserEnabled);
                            }
                            if (isEraserEnabled) {
                                drawCell(newCell, cellSize, white);
                            } else {
                                drawCell(newCell, cellSize, lightCoral);
                            }
                            field.setImageBitmap(bitmap);
                        }
                    }
                    break;
                case 2:
                    Point point1 = new Point((int) motionEvent.getX(0), (int) motionEvent.getY(0));
                    Point point2 = new Point((int) motionEvent.getX(1), (int) motionEvent.getY(1));
                    int distance = getDistanceBetweenPoints(point1, point2);
                    if (firstTouchDistance < 0) {
                        firstTouchDistance = distance;
                    }
                    float coefficient = (float)distance / firstTouchDistance;
                    int newCellSize = (int)(cellSize * coefficient);
                    if (isNewSizeSuitable(newCellSize) && newCellSize >= 15 * dp) {
                        widthSeekBar.setMax(screenWidth / newCellSize);
                        heightSeekBar.setMax(screenHeight / newCellSize);
                        createField(newCellSize, widthSeekBar.getProgress(), heightSeekBar.getProgress());
                        tempCellSize = newCellSize;
                        drawMatrix(newCellSize);
                    }
                    break;
            }
            if (pointerCount != 2) {
                cellSize = tempCellSize;
                firstTouchDistance = -1;
            }
        }
        return true;
    }

    private void createField(int cellSize, int fieldWidth, int fieldHeight) {
        bitmap = Bitmap.createBitmap(fieldWidth * cellSize, fieldHeight * cellSize, Bitmap.Config.ARGB_8888);
        Paint pen = new Paint();
        pen.setColor(darkGray);
        pen.setAntiAlias(true);
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeWidth(cellSize / 50f);

        canvas = new Canvas(bitmap);
        for (int i = 0; i <= canvas.getWidth(); i += cellSize) {
            canvas.drawLine(i, 0, i, canvas.getHeight(), pen);
        }
        for (int i = 0; i <= canvas.getHeight(); i += cellSize) {
            canvas.drawLine(0, i, canvas.getWidth(), i, pen);
        }
        if (matrix == null || fieldWidth != matrix.getWidth() || fieldHeight != matrix.getHeight()) {
            matrix = new LifeMatrix(fieldWidth, fieldHeight);
        }
        drawMatrix(cellSize);
        field.setImageBitmap(bitmap);
    }

    private void drawCell(Point cell, int cellSize, int cellColor) {
        Paint pen = new Paint();
        pen.setColor(cellColor);
        pen.setAntiAlias(true);
        Point leftTop = new Point(cell.x * cellSize + 1, cell.y * cellSize + 1);
        canvas.drawRect(leftTop.x, leftTop.y, leftTop.x + cellSize - 2, leftTop.y + cellSize - 2, pen);
    }

    private void drawMatrix(int cellSize) {
        for (int i = 0; i < matrix.getWidth(); i++) {
            for (int j = 0; j < matrix.getHeight(); j++) {
                if (matrix.getCellPreviousState(i, j) != matrix.getCell(i, j)
                        || timelapse == null
                        || !timelapse.isAlive()) {
                    drawCell(new Point(i, j), cellSize, matrix.getCell(i, j) ? lightCoral : white);
                    field.setImageBitmap(bitmap);
                }
            }
        }
    }

    private Point getCurrentCell (Point fingerLocation) {
        return new Point(fingerLocation.x / cellSize, fingerLocation.y / cellSize);
    }

    private int getDistanceBetweenPoints(Point point1, Point point2) {
        return (int)Math.sqrt((point2.x - point1.x) * (point2.x - point1.x) + (point2.y - point1.y) * (point2.y - point1.y));
    }

    public void onClickNewGeneration(View view) {
        matrix.updateGeneration();
        drawMatrix(cellSize);
    }

    public void onClickTimeLapse(View view) {
        if (timelapse != null && timelapse.isAlive()) {
            timelapse.interrupt();
            setEnabledSeekBarsAndEditTexts(true);
            setEnabledButtons(true);
            view.setForeground(playIcon);
        } else {
            setEnabledSeekBarsAndEditTexts(false);
            setEnabledButtons(false);
            view.setForeground(pauseIcon);
            timelapse = new TlThread("timelapse");
            timelapse.start();
        }
    }

    private void setEnabledSeekBarsAndEditTexts(boolean isEnabled) {
        widthTextNumber.setEnabled(isEnabled);
        heightTextNumber.setEnabled(isEnabled);
        widthSeekBar.setEnabled(isEnabled);
        heightSeekBar.setEnabled(isEnabled);
    }

    private void setEnabledButtons(boolean isEnabled) {
        findViewById(R.id.GenerationUpdateButton).setEnabled(isEnabled);
        findViewById(R.id.EraserButton).setEnabled(isEnabled);
        if (isEnabled) {
            findViewById(R.id.GenerationUpdateButton).setBackgroundColor(lightCoral);
            setEraserColor();
        } else {
            findViewById(R.id.GenerationUpdateButton).setBackgroundColor(gray);
            findViewById(R.id.EraserButton).setBackgroundColor(gray);
        }
    }

    public void onClickEraser(View view) {
        isEraserEnabled = !isEraserEnabled;
        setEraserColor();
    }

    public void onClickToggleVisibility(View view) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)view.getLayoutParams();
        int state;
        if (findViewById(R.id.ToolsLinearLayout).getVisibility() == View.VISIBLE) {
            params.bottomMargin = (int)(16 * dp);
            state = View.INVISIBLE;
            view.setForeground(arrowUpIcon);
        } else {
            params.bottomMargin = (int)(240 * dp);
            state = View.VISIBLE;
            view.setForeground(arrowDownIcon);
        }
        findViewById(R.id.ToolsLinearLayout).setVisibility(state);
        view.setLayoutParams(params);
    }

    private void setEraserColor() {
        if (isEraserEnabled) {
            findViewById(R.id.EraserButton).setBackgroundColor(lightCoral);
        } else {
            findViewById(R.id.EraserButton).setBackgroundColor(lightRed);
        }
    }

    public void onClickClearField(View view) {
        if (timelapse != null && timelapse.isAlive()) {
            timelapse.interrupt();
            setEnabledSeekBarsAndEditTexts(true);
            setEnabledButtons(true);
            findViewById(R.id.TimelapseButton).setForeground(playIcon);
        }
        matrix.clearField();
        drawMatrix(cellSize);
    }

    public void onClickSettings(View view) {
        if (settingsLayout.getVisibility() == View.VISIBLE) {
            settingsLayout.setVisibility(View.INVISIBLE);
        } else {
            settingsLayout.setVisibility(View.VISIBLE);
        }
    }

    public void onClickCyclicalBorders(View view) {
        Switch flatTorusSwitch = (Switch) view;
        Drawable trackDrawable = flatTorusSwitch.getTrackDrawable(),
            thumbDrawable = flatTorusSwitch.getThumbDrawable();
        if (flatTorusSwitch.isChecked()) {
            trackDrawable.setTint(lightCoral);
            thumbDrawable.setTint(lightCoral);
        } else {
            trackDrawable.setTint(darkGray);
            thumbDrawable.setTint(darkenedWhite);
        }
        isTorus = flatTorusSwitch.isChecked();
    }

    @Override
    protected void onDestroy() {
        timelapse.canTimelapseRun = false;
        timelapse.interrupt();
        super.onDestroy();
    }

    class TlThread extends Thread implements Runnable {
        private boolean canTimelapseRun = true;
        public TlThread(String name) {
            super(name);
        }

        public void run() {
            try {
                while (canTimelapseRun) {
                    matrix.updateGeneration();
                    drawMatrix(cellSize);
                    Thread.sleep(timelapseStepDelay);
                }
            } catch (InterruptedException ex) {
                canTimelapseRun = false;
            }
        }
    }
}

class LifeMatrix {
    private boolean[][] field;
    private boolean[][] previousField;

    public LifeMatrix(int width, int height) {
        field = new boolean[width][height];
        previousField = new boolean[width][height];
    }

    public int getWidth () {
        return field.length;
    }

    public int getHeight () {
        return field[0].length;
    }

    public boolean getCell(int i, int j) {
        return field[i][j];
    }

    public boolean getCellPreviousState(int i, int j) {
        return previousField[i][j];
    }

    public void setCell(int i, int j, boolean isAlive) {
        field[i][j] = isAlive;
    }

    public void updateGeneration() {
        previousField = field.clone();
        boolean[][] tempMatrix = new boolean[getWidth()][getHeight()];
        for (int i = 0; i < getWidth(); i++)
        {
            for (int j = 0; j < getHeight(); j++)
            {
                tempMatrix[i][j] = field[i][j];
                int localsCount = field[i][j] ? -1 : 0;

                // Here the algorithm checks element's locals
                // If the element is the "border" element, we don't check it's non - existent locals
                if (MainActivity.isTorus) {
                    for (int horOffset = -1; horOffset < 2; horOffset++)
                        for (int verOffset = -1; verOffset < 2; verOffset++)
                        {
                            int horIndex = i + horOffset;
                            int verIndex = j + verOffset;
                            if (i == 0 && horOffset == -1) {
                                horIndex = getWidth() - 1;
                            }
                            if (i == getWidth() - 1 && horOffset == 1) {
                                horIndex = 0;
                            }
                            if (j == 0 && verOffset == -1) {
                                verIndex = getHeight() - 1;
                            }
                            if (j == getHeight() - 1 && verOffset == 1) {
                                verIndex = 0;
                            }
                            localsCount += (field[horIndex][verIndex]) ? 1 : 0;
                        }
                } else {
                    for (int horOffset = i == 0 ? 0 : -1; horOffset < (i == getWidth() - 1 ? 1 : 2); horOffset++) {
                        for (int verOffset = j == 0 ? 0 : -1; verOffset < (j == getHeight() - 1 ? 1 : 2); verOffset++) {
                            localsCount += field[i + horOffset][j + verOffset] ? 1 : 0;
                        }
                    }
                }
                tempMatrix[i][j] = (localsCount == 2 && tempMatrix[i][j]) || localsCount == 3;
            }
        }
        field = tempMatrix;
    }

    public void clearField() {
        field = new boolean[getWidth()][getHeight()];
    }
}