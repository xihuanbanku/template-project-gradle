package com.lemon;

import ev3dev.actuators.LCD;
import ev3dev.actuators.ev3.EV3Led;
import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.Battery;
import ev3dev.sensors.Button;
import lejos.hardware.Key;
import lejos.hardware.KeyListener;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.port.MotorPort;
import lejos.robotics.Color;
import lejos.utility.Delay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FishFeeder {
    public static Logger LOGGER = LoggerFactory.getLogger(FishFeeder.class);
    public static final int ROTATE_SPEED = 180;

    private EV3LargeRegulatedMotor motorA;
    private EV3LargeRegulatedMotor motorB;
    private GraphicsLCD lcd;
    private static int alarmHour = 8;
    private static int alarmMinute = 30;

    private static int settingFlag = 0;  // 0=hour  1=minute
    private int feedCount = 0;
    private boolean fed = false;

    public FishFeeder() {
        try {
            init();
            startTimer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() throws IOException {
        new EV3Led(EV3Led.LEFT).setPattern(1);
        new EV3Led(EV3Led.RIGHT).setPattern(3);
        LOGGER.info("Test LCD");
        lcd = LCD.getInstance();
        lcd.setAutoRefresh(true);
        lcd.setColor(Color.BLACK);
//        lcd.refresh();
//        Delay.msDelay(3000);
        LOGGER.info("Creating Motor A & B");
        motorA = new EV3LargeRegulatedMotor(MotorPort.A);
        motorB = new EV3LargeRegulatedMotor(MotorPort.B);
        LOGGER.info("Defining the Stop mode");
        motorA.brake();
        motorB.brake();

        LOGGER.info("Defining motor speed");
        motorA.setSpeed(ROTATE_SPEED);
        motorB.setSpeed(ROTATE_SPEED);
        Button.UP.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(Key k) {
                if (settingFlag == 0) {
                    alarmHour++;
                    if (alarmHour >= 24) {
                        alarmHour = 0;
                    }
                } else {
                    alarmMinute++;
                    if (alarmMinute >= 60) {
                        alarmMinute = 0;
                    }
                }
            }

            @Override
            public void keyReleased(Key k) {

            }
        });
        Button.DOWN.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(Key k) {
                if (settingFlag == 0) {
                    alarmHour--;
                    if (alarmHour <= -1) {
                        alarmHour = 23;
                    }
                } else {
                    alarmMinute--;
                    if (alarmMinute <= -1) {
                        alarmMinute = 59;
                    }
                }
            }

            @Override
            public void keyReleased(Key k) {

            }
        });
        Button.RIGHT.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(Key k) {
                if (settingFlag == 0) {
                    settingFlag = 1;
                } else {
                    settingFlag = 0;
                }
            }

            @Override
            public void keyReleased(Key k) {

            }
        });
    }

    public static void main(final String[] args) {

        new FishFeeder();

        //To Stop the motor in case of pkill java for example
//        Runtime.getRuntime().addShutdownHook(new CorruptMonitor());

        System.exit(0);
    }

    //获取当前系统时间
    private void startTimer() {
        new EV3Led(EV3Led.LEFT).setPattern(0);
        new EV3Led(EV3Led.RIGHT).setPattern(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        while (true) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR_OF_DAY, 8);

            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            String stringNow = sdf.format(cal.getTime());
            float voltage = Battery.getInstance().getVoltage();

            lcd.drawString(stringNow, 10, 10, 0);
            lcd.drawString("Votage: [" + voltage + "]", 10, 20, 0);
            lcd.drawString("Alarm: [" + alarmHour + "-" + alarmMinute + "]", 10, 30, 0);
            lcd.refresh();
            Delay.msDelay(1000);
//            lcd.drawRect(0, 0, lcd.getWidth(), 30);
            // 达到闹铃时间
            if ((hour == alarmHour || hour == (alarmHour + 12) % 24) && minute == alarmMinute) {
                feed(stringNow);
            } else if (fed) {
                fed = false;
            }
            LOGGER.info(stringNow);
            clearTop();
        }
    }

    public void clearTop() {
        lcd.setColor(255, 255, 255);
        lcd.fillRect(0, 0, lcd.getWidth(), lcd.getHeight() / 2);
        lcd.setColor(0, 0, 0);
    }

    public void clearBottom() {
        lcd.setColor(255, 255, 255);
        lcd.fillRect(0, lcd.getHeight() / 2, lcd.getWidth(), lcd.getHeight() / 2);
        lcd.setColor(0, 0, 0);
    }

    private void feed(String now) {
        if (!fed) {
            clearBottom();
            feedCount++;
            LOGGER.info("Alarm trigger");
            lcd.drawString("Alarm trigger[" + feedCount + "]", 10, lcd.getHeight() / 2 + 10, 0);
            lcd.drawString("[" + now + "]", 10, lcd.getHeight() / 2 + 20, 0);
            motorA.rotate(360, true);
            motorB.rotate(360, true);

            LOGGER.info("Stop motors");
            lcd.drawString("Stop motors", 10, lcd.getHeight() / 2 + 30, 0);
            fed = true;
        }
    }
}
