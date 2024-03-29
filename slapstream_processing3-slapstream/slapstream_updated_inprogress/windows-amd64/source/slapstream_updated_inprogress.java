/* autogenerated by Processing revision 1293 on 2024-02-06 */
import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import SimpleOpenNI.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class slapstream_updated_inprogress extends PApplet {

// Slapstream
// By Andrew Cerrito
// Stars and parallax motion modified very slightly from William Smith's sketch on OpenProcessing:
// http://www.openprocessing.org/sketch/63495
// Kinect code adapted from examples from Making Things See by Greg Borenstein
// Thanks to:
// Dan Shiffman, Mark Kleback, Genevieve Hoffman, Ben Smith


SimpleOpenNI kinect;

//debug mode switch - keyboard controls & console debugging info
boolean debugMode = false;

//Table for recording and reading high scores
boolean scoreLogged = false;
Table scoreTable;
int gameScore = 0;
int highScore, scoreRank, totalScores;

// info to track only the calibrated user
int calibratedUser;
boolean isUserCalibrated = false;

Hero hero;
ArrayList<Obstacle> obstacles = new ArrayList();
Star[] stars;
Meter leftMeter;
Meter rightMeter;

PImage ship;
PImage[] asteroids;


// For the star movement:
PVector offset;

float leftHandMagnitude, rightHandMagnitude;

int heroLives, randX;

// time when last game ended - used for asteroid speed calculation
long millisSinceGameEnd = 0;

long millisSinceTrack = 0;

// Style-related variables
int c1 = color(0, 0, 0);
int green = color(0, 255, 0);
int blue = color(0, 0, 255);
PFont pixelFont;
PFont defaultFont;
PImage psipose;

boolean titleScreen, p1ready, p2ready, restartOK, getGameEndTime;

//Frame counter to give a short delay after detecting 1P to detect 2P
int frameCounter;

public void setup() {
  /* size commented out by preprocessor */;
  //smooth();
  background(c1);

  scoreTable = loadTable("data/scoretable.csv", "header");
  scoreTable.setColumnType("scorelist", Table.INT);


  //load assets & fonts
  psipose = loadImage("Psiyellow.png");
  pixelFont = createFont("C64Pro", 24, true);
  defaultFont = createFont("SansSerif", 12, true);

  heroLives = 5;

  randX = 10;
  titleScreen = true;
  p1ready = false;
  restartOK = false;
  getGameEndTime = true;
  frameCounter = 0;

  // define hero, obstacle, and stars
  hero = new Hero(600/2, height-80, 85, green); //SET TO 600 - CHANGE BACK LATER

  // load ship image
  ship = loadImage("orangeship.png");
  ship.resize(PApplet.parseInt(hero.w*1.4f), PApplet.parseInt(hero.w*1.4f));

  // load slap power meters
  leftMeter = new Meter(700, 250, 50, 400, "Left Hand", "left");
  rightMeter = new Meter(875, 250, 50, 400, "Right Hand", "right");



  // create obstacles
  for (int i =0; i < 5; i++) {
    int randX = (int) random (0, 600);
    Obstacle obst = new Obstacle(randX, 10);
    obst.imageInit();
    obst.imageSelect();
    obstacles.add(obst);
    println("Obstacle " + i + " loaded");
  }

  stars = new Star[width];
  for (int i = 0; i < stars.length; i ++) stars[i] = new Star();
  offset = new PVector(width / 2, height / 2);

  //Kinect initialization
  kinect = new SimpleOpenNI(this);
  kinect.enableDepth();
  kinect.enableUser();
  //kinect.enableUser(SimpleOpenNI.SKEL_PROFILE_ALL);
  frameRate(30);
}

public void restart() {
  heroLives = 5;
  randX = 10;
  titleScreen = true;
  p1ready = false;
  restartOK = false;
  isUserCalibrated = false;
  getGameEndTime = true;
  frameCounter = 0;

  hero = new Hero(600/2, height-80, 85, green); //SET TO 600 - CHANGE BACK LATER

  // load ship image
  ship = loadImage("orangeship.png");
  ship.resize(PApplet.parseInt(hero.w*1.4f), PApplet.parseInt(hero.w*1.4f));

  // reset obstacle parameters 
  for (int i = 0; i < obstacles.size (); i++) {
    Obstacle obst = obstacles.get(i);
    obst.y = -obst.rad;
    obst.obstSpeed = 0;
    obst.speedModifier = random(-2, 2);
  }
  stars = new Star[width];
  for (int i = 0; i < stars.length; i ++) stars[i] = new Star();
  offset = new PVector(width / 2, height / 2);
}

public void draw() {
  pushStyle();
  //  background(c1);
  noStroke();
  fill(c1);
  rect(0, 0, 600, 875);
  c1 = color(0, 0, 0);
  starField();
  leftMeter.display();
  rightMeter.display();
  popStyle();

  if (titleScreen) {
    millisSinceGameEnd = millis(); // don't keep elapsed game time until game starts
    gameScore = 0; // don't keep score until game starts
    scoreLogged = false;
    kinectDraw();
    pushStyle();
    fill(255, 255, 0);
    textFont(pixelFont, 60);
    textAlign(CENTER);
    text("Slapstream", 300, height/2-150);
    textFont(pixelFont, 24);
    text("Stand so you can see", 300, height/2 - 50);
    text("your upper body in the frame.", 300, height/2);
    PImage depth = kinect.depthImage();
    PImage depth2 = depth.get();
    depth2.resize(PApplet.parseInt(640*.48f), PApplet.parseInt(480*.48f));
    imageMode(CENTER);
    image(depth2, 300, height/2+150);

    popStyle();


    // if player 1 is ready, display a message, wait a little bit, and start the game
    if (p1ready) {
      pushStyle();
      fill(green);
      textAlign(CENTER);
      textFont(pixelFont, 20);
      text("Player detected - beginning game...", 300, height-50);
      popStyle();
      frameCounter++;
    }
    if (frameCounter > 100) {
      titleScreen = false;
    }
  }


  // if user is detected, game begins
  if (!titleScreen) {


    // Right now, game is set to end if either player loses all lives. Change this later.
    if (heroLives > 0) {
      hero.speedCalc();
      kinectDraw();
      hero.display();
      hero.moveCheck();

      for (int i = 0; i < obstacles.size (); i++) {
        Obstacle obst = obstacles.get(i);
        obst.display();
        obst.move();
        hero.collideDetect(obst.x, obst.y, obst.rad);
      }

      if (obstacles.size() > 20) {
        obstacles.remove(0);
      }

      // update the game's score
      updateScore();

      // draw onscreen info last so it can't get covered by falling obstacles
      fill(255);
      pushStyle();
      textFont(pixelFont, 24); 
      text("Lives: " + heroLives, 10, 30);
      textAlign(RIGHT);
      text("Score: " + gameScore, 590, 30);
      textFont(defaultFont, 36);
      //    text (frameRate, width-150, height-90);
      //  text (topSpeed, width-60, height-100);
      popStyle();

      // FOR DEBUG - visualize speed vectors onscreen for P1
      //      hero.speedVectorDraw();
    } else {  // if zero lives remaining:

      if (getGameEndTime) {
        millisSinceGameEnd = millis();
        getGameEndTime = false;
      }

      if (!scoreLogged) {
        logScore(gameScore);
        endgameScoreInfo(gameScore);
        scoreLogged = true;
      }
      kinectDraw();
      background(0);
      starField();
      pushStyle();
      fill(255, 255, 0);
      textFont(pixelFont, 48);
      textAlign(CENTER);
      text("GAME OVER", 300, height/2);
      textFont(pixelFont, 20);
      fill(0, 255, 0);
      text("Your Score: " + gameScore, 300, 50);
      text("Today's High Score: " + highScore, 300, 100);
      text("Your Player Rank: " + scoreRank + " out of " + totalScores, 300, 150);
      fill(255, 255, 0);
      text("Please clear the game area", 300, height/2+100);
      text("to allow the Kinect to recalibrate.", 300, height/2+150);
      popStyle();


      //IntVector userList = new IntVector();rr
      //kinect.getUsers(userList);
      //println(userList.size());

      if ((millis()> millisSinceGameEnd + 5000) && restartOK) {
        // millisSinceGameEnd = millis();
        print("CAN RESTART NOW!!!!!");
        kinect.stopTrackingSkeleton(calibratedUser);
        restart();
      }
    }
  }
}






// ******** STAR FIELD FUNCTION ********

public void starField() {
  for (int i = 0; i < stars.length; i ++) stars[i].display();

  // Make stars float down from top of screen
  //  PVector angle = new PVector(mouseX - width / 2, mouseY - height / 2);
  PVector angle = new PVector(0, 0);
  angle.y--;
  angle.normalize();
  //angle.mult(dist(width / 2, height / 2, mouseX, mouseY) / 50);

  // this multiplier controls speed of stars
  angle.mult(5); 

  offset.add(angle);
}





// ******** KINECT FUNCTIONS ********



public void kinectDraw() {
  pushStyle();
  kinect.update();
  imageMode(CORNER);
  //image(kinect.depthImage(), 600, 100);
  popStyle();

  IntVector userList = new IntVector();
  kinect.getUsers(userList);

  // disabling 2nd player detection for now until base 1p game is smoothed out  
  /*
  if (userList.size() > 1) {
   int user1 = userList.get(0);
   int user2 = userList.get(1);
   if (kinect.isTrackingSkeleton(user1)) {
   hero.drawSkeleton(user1);
   p1ready = true;
   }
   if (kinect.isTrackingSkeleton(user2)) {
   hero2.drawSkeleton(user2);
   p2ready = true;
   }
   }
   else if (userList.size() > 0) {
   int userId = userList.get(0);
   if (kinect.isTrackingSkeleton(userId)) {
   hero.drawSkeleton(userId);
   p1ready = true;
   }
   }
   */

  if (userList.size() > 0) {
    int userId = userList.get(0);
    if (kinect.isTrackingSkeleton(userId)) {
      hero.drawSkeleton(userId);
      p1ready = true;
      //print("P1 is ready!");
    }
  }
}

// ******** GETTING SCORES **********
public void updateScore() {
  gameScore += (int) ((millis()-millisSinceGameEnd)/900)*heroLives;
  //println(gameScore);
}

public void logScore(int score) {
  TableRow newEntry = scoreTable.addRow();
  newEntry.setInt("scorelist", score);
  saveTable(scoreTable, "data/scoretable.csv");
  println("new score added: " + score);
}

public void endgameScoreInfo(int score) {
  scoreTable.sortReverse(PApplet.parseInt("scorelist")); // sort scores in descending order

  // retrieve highest score in list
  TableRow highestRow = scoreTable.getRow(0);
  highScore = highestRow.getInt("scorelist");

  // determine rank in list of current game's score
  for (int i=0; i < scoreTable.getRowCount (); i++) {
    TableRow searchedRow = scoreTable.getRow(i);
    if (searchedRow.getInt("scorelist") == score) scoreRank = i+1;
  }
  // determine total amount of scores recorded
  totalScores = scoreTable.getRowCount();
}


// ******** KINECT USER TRACKING FUNCTIONS ********

public void onNewUser(SimpleOpenNI kinect, int userId) {
  if (!isUserCalibrated) {
    println("start " + userId + " pose detection");
    kinect.startTrackingSkeleton(userId); 
    calibratedUser = userId;  // new
    isUserCalibrated = true;  //new
    println("start skeleton tracking");
  } else {
    println("user already calibrated, this is just noise!");
  }
}

/*
void onEndCalibration(int userId, boolean successful) {
 if (successful) {
 println("User " + userId + " calibrated !!!");
 calibratedUser = userId;
 isUserCalibrated = true;
 kinect.startTrackingSkeleton(userId);
 println("DID IT");
 } else {
 println("  Failed to calibrate " + userId + " !!!");
 kinect.startTrackingSkeleton(userId);
 }
 }
 */

/*
void onStartPose(String pose, int userId) {
 println("Started pose for user " + userId);
 kinect.stopPoseDetection(userId);
 kinect.requestCalibrationSkeleton(userId, true);
 }
 */

public void onLostUser(SimpleOpenNI kinect, int userId)
{
  if (userId == calibratedUser) {
    println("USER LOST: USER ID -  " + userId);
    kinect.stopTrackingSkeleton(calibratedUser); // new
    isUserCalibrated = false;
    restartOK = true;
    println("RESTART OK");
  }
}

public void keyPressed() {
  if (key == 'R'|| key == 'r') {
    restartOK = true;
    isUserCalibrated = false;
  }
}
class Hero {
  float x, y, w;
  int playerColor;
  float rad;

  //collision detection booleans  
  boolean collideState = false;
  boolean prevState = false;
  int hitCount = 0;
  float timeWhenHit = 0;

  // Vectors to track hand positions and velocity  
  PVector rhand = new PVector(width/2, height/2);
  PVector prhand = new PVector(width/2, height/2);
  PVector rhandvel = new PVector(0, 0);

  PVector lhand = new PVector(width/2, height/2);
  PVector plhand = new PVector(width/2, height/2);
  PVector lhandvel = new PVector(0, 0);


  Hero () {
  } // default constructor needed for inheritance

  Hero (float tx, float ty, float tw, int tplayerColor) {
    x = tx;
    y = ty;
    w = tw;
    rad = w/2;
    playerColor = tplayerColor;
  }

  public void display() {
    pushStyle();
    noStroke();
    noFill();
    ellipse(x, y, w, w);
    imageMode(CENTER);
    image(ship, x, y);
    popStyle();
  }


  // Calculates velocity of hands and interpolates it for smoothness
  public void speedCalc() {
    PVector rvelocity = PVector.sub(rhand, prhand);
    rhandvel.x = lerp(rhandvel.x, rvelocity.x, 0.4f);
    rhandvel.y = lerp(rhandvel.y, rvelocity.y, 0.4f);
    PVector lvelocity = PVector.sub(lhand, plhand);
    lhandvel.x = lerp(lhandvel.x, lvelocity.x, 0.4f);
    lhandvel.y = lerp(lhandvel.y, lvelocity.y, 0.4f);
  }

  // Draws represention of speed vectors onscreen
  public void speedVectorDraw() {
    strokeWeight(1);
    stroke(0, 255, 0);
    pushMatrix();
    translate(width/2, height/2);
    scale(10);

    stroke(0, 255, 0);
    line(0, 0, rhandvel.x, rhandvel.y);

    stroke(255, 0, 0);
    line(0, 0, lhandvel.x, lhandvel.y);

    popMatrix();
  }


  // move hero if user is slapping and sprite is within bounds
  // inverted movement enabled: 
  // change x >,< direction and +,- operator for non-inverted
  public void moveCheck() {

    if (leftHandMagnitude <= 300 && x<=600) {
      leftMeter.updatePower(lhandvel.mag());
      x = (x +(lhandvel.mag()/3));
    }
    if (rightHandMagnitude <= 300 && x>=0) {
      rightMeter.updatePower(rhandvel.mag());
      x = PApplet.parseInt (x-(rhandvel.mag()/3));
    }
    // debug mode - keyboard controls so i don't have to slap myself during testing
    if (debugMode) {
      if (keyPressed) {
        if (key == 'k' || key == 'K') x -= 10;
        if (key == 'l' || key =='L') x += 10;
      }
    }
  }

  public void collideDetect (float obstX, float obstY, float obstRad) {
    // time based approach: when hit, take a life away, but
    // make hero invulnerable for 2 seconds after hit.
    float distFromObst = dist(x, y, obstX, obstY);
    if (distFromObst < rad + obstRad) {
      c1 = color(255, 0, 0);
      if (millis() - timeWhenHit > 2000) {
        heroLives--;
        timeWhenHit = millis();
      }
    }
  }


  public void drawSkeleton(int userId) {
    PVector leftHand = new PVector();
    PVector rightHand = new PVector();
    PVector head = new PVector();

    kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HAND, rightHand);
    kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_HAND, leftHand);
    kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_HEAD, head);

    // NEW: get current value for hand and store previous value
    prhand = rhand;
    rhand = rightHand;

    plhand = lhand;
    lhand = leftHand;

    // subtract hand vectors from head to get difference vectors
    PVector rightHandVector = PVector.sub(head, rightHand);
    PVector leftHandVector = PVector.sub(head, leftHand);

    // calculate the distance and direction of the difference vector
    rightHandMagnitude = rightHandVector.mag();
    leftHandMagnitude = leftHandVector.mag();
    // this is for unit vectors - uncomment it if you need to do something with direction
    //      rightHandVector.normalize();
    //      leftHandVector.normalize();


    // draw a line between the two hands
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_RIGHT_HAND);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_LEFT_HAND);

    // display info onscreen for testing
    if (debugMode) {
      pushMatrix();
      pushStyle();
      fill(255, 0, 0);
      textFont(pixelFont, 18);
      text("left: " + lhandvel.mag(), 10, height-200);
      text("right: " + rhandvel.mag(), 400, height-200);
      popMatrix();
      popStyle();
    }
  }
}
class Meter extends Hero {
  int x, y, w, h;
  String label;
  String textLoc;
 
  Meter(int tx, int ty, int tw, int th, String t_label, String t_textLoc) {
    x = tx;
    y= ty;
    w = tw;
    h = th;
    label =(t_label);
    textLoc = t_textLoc;
  }
  
  public void display() {
    // display meter outlines
    pushStyle();
    stroke(255);
    noFill();
    rect(x,y,w,h);
    noStroke();
    fill(0,50);
    rect(x,y,w,h);
    fill(255,255,0);
    
    // display text
    textAlign(CENTER);
    rectMode(CENTER);
    noStroke();
    fill(0);
    rect(x+(w/2),y-30,160,30);
    textFont(pixelFont, 16);
    fill(255,255,0);
    text(label, x+(w/2),y-20);
    textFont(pixelFont, 24);
    text("Power Gauge", 810, y-75);
    
    // clear edge of game screen in case obstacles overlap
    rectMode(CORNER);
    fill(0);
    rect(600,0,65,height);
    
    popStyle();
  }
  
  public void updatePower(float powerValue) {
    powerValue = map(powerValue, 0, 80, 0, h);
    if (powerValue > h) powerValue = h;
    fill(0,255,0);
    rect(x,y+h,w,-powerValue);
  }
  
}
class Obstacle {

  PImage[] asteroids;
  PImage currentGraphic;
  int maxSize = 150; // dictates maximum size of obstacle
  int x; 
  float y;
  int w = (int) random(30, maxSize);
  float rad;
  float obstSpeed = 0;
  float speedModifier = random(-2, 2); // makes the obstacles fall at slightly different speed, more staggered
  boolean graphicSelected = false;

  Obstacle (int tx, float ty) {
    x = tx;
    y = ty;
    rad = w/2;
  }

  public void imageInit() {
    asteroids = new PImage[7];
    for (int i = 1; i < 8; i++) {
      println("image " + i + " loaded.");
      // Use nf() to number format 'i' into four digits
      String filename = "asteroid_" + nf(i, 4) + ".png";
      println(filename);
      asteroids[i-1] = loadImage(filename);
    }
  }

  public void imageSelect() {
    if (graphicSelected == false) {
      int imageSelector = (int)random(1, 8);
      switch(imageSelector) {
      case 1:
        //    image(asteroids[0], x, y);
        asteroids[0].resize(PApplet.parseInt(w*1.4f), PApplet.parseInt(w*1.4f));
        currentGraphic = asteroids[0];
        break;
      case 2:
        //      image(asteroids[1], x, y);
        asteroids[1].resize(PApplet.parseInt(w*1.3f), PApplet.parseInt(w*1.3f));
        currentGraphic = asteroids[1];
        break;
      case 3:
        //      image(asteroids[2], x, y);
        asteroids[2].resize(PApplet.parseInt(w*1.1f), PApplet.parseInt(w*1.1f));
        currentGraphic = asteroids[2];
        break;
      case 4:
        //      image(asteroids[3], x, y);
        asteroids[3].resize(PApplet.parseInt(w*1.1f), PApplet.parseInt(w*1.1f));
        currentGraphic = asteroids[3];
        break;
      case 5:
        //      image(asteroids[4], x, y);
        asteroids[4].resize(PApplet.parseInt(w*1.2f), PApplet.parseInt(w*1.2f));
        currentGraphic = asteroids[4];
        break;
      case 6:
        //      image(asteroids[5], x, y);
        asteroids[5].resize(PApplet.parseInt(w*1.1f), PApplet.parseInt(w*1.1f));
        currentGraphic = asteroids[5];
        break;
      case 7:
        //      image(asteroids[6], x, y);
        asteroids[6].resize(PApplet.parseInt(w*1.1f), PApplet.parseInt(w*1.1f));
        currentGraphic = asteroids[6];
        break;
      }
    }
    graphicSelected = true;
  }


  public void display() {
    pushStyle();
    noFill();
    noStroke();
    //    fill(255, 0, 0, 100);
    //    stroke(255);
    imageMode(CENTER);
    image(currentGraphic, x, y);
    ellipse(x, y, w, w);
    popStyle();
  }

  public void move() {
    obstSpeed = (float) (millis()-millisSinceGameEnd)/9000; // make speed increase the longer the game goes on
    y= y + 4 + obstSpeed + speedModifier; // move down the screen
    if (y >= height + rad) { // if circle leaves bottom of screen:
      y = (int) -rad; // reset to top of screen
      x = (int) random(0, 600); // get a new random x-position - SET TO 600 FOR NOW, CHANGE BACK LATER
      w = (int) random(30, maxSize); // get a new random size
      graphicSelected = false; // get a new asteroid graphic
      imageSelect();
      rad = w/2; // correct the radius variable with the new width
    }
  }
  
}
// Modified very slightly from William Smith's sketch on OpenProcessing:
// http://www.openprocessing.org/sketch/63495

class Star {
  //Location
  PVector loc;
  //Size
  int size;
  //Brightness
  int bright;

  Star() {
    //Randomize all of the values
    size = (int) random(1, 6);
//    loc = new PVector(random(width * map(size, 1, 7, 7, 1)), random(height * map(size, 1, 7, 7, 1)));
    loc = new PVector(random(600 * map(size, 1, 7, 7, 1)), random(height * map(size, 1, 7, 7, 1)));
    bright = (int) random(150, 215);
  }

  public void display() {
    pushStyle();

    //Setup the style
    stroke(bright);
    strokeWeight(size);

    //Find the actual location and constrain it to within the bounds of the screen
    // (Switch commented out lines with ones below them once Kinect display is offscreen)

//    int x = (int) (((loc.x - offset.x) * size / 8)) % width;
    int x = (int) (((loc.x - offset.x) * size / 8)) % 600;
    int y = (int) (((loc.y - offset.y) * size / 8)) % height;
//    if (x < 0) x += width;
    if (x < 0) x += 600;
    if (y < 0) y += height;

    //Display the point
    point(x, y);
    //rectMode(CENTER);
    //rect(x,y, size, size);

    popStyle();
  }
}


  public void settings() { size(1050, 875, P2D); }

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "slapstream_updated_inprogress" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
