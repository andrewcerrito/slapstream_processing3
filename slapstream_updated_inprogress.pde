// Slapstream
// By Andrew Cerrito
// Stars and parallax motion modified very slightly from William Smith's sketch on OpenProcessing:
// http://www.openprocessing.org/sketch/63495
// Kinect code adapted from examples from Making Things See by Greg Borenstein
// Thanks to:
// Dan Shiffman, Mark Kleback, Genevieve Hoffman, Ben Smith

import SimpleOpenNI.*;
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
color c1 = color(0, 0, 0);
color green = color(0, 255, 0);
color blue = color(0, 0, 255);
PFont pixelFont;
PFont defaultFont;
PImage psipose;

boolean titleScreen, p1ready, p2ready, restartOK, getGameEndTime;

//Frame counter to give a short delay after detecting 1P to detect 2P
int frameCounter;

void setup() {
  size(1050, 875, P2D);
  //smooth();
  background(c1);

  scoreTable = loadTable("data/scoretable.csv", "header");
  scoreTable.setColumnType("scorelist", Table.INT);


  //load assets & fonts
  psipose = loadImage("Psiyellow.png");
  pixelFont = createFont("C64Pro-Style", 24, true);
  defaultFont = createFont("SansSerif", 12, true);

  heroLives = 1;

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
  ship.resize(int(hero.w*1.4), int(hero.w*1.4));

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

void restart() {
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
  ship.resize(int(hero.w*1.4), int(hero.w*1.4));

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

void draw() {
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
    depth2.resize(int(640*.48), int(480*.48));
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

void starField() {
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



void kinectDraw() {
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
void updateScore() {
  gameScore += (int) ((millis()-millisSinceGameEnd)/900)*heroLives;
  //println(gameScore);
}

void logScore(int score) {
  TableRow newEntry = scoreTable.addRow();
  newEntry.setInt("scorelist", score);
  saveTable(scoreTable, "data/scoretable.csv");
  println("new score added: " + score);
}

void endgameScoreInfo(int score) {
  scoreTable.sortReverse(int("scorelist")); // sort scores in descending order

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

void onNewUser(SimpleOpenNI kinect, int userId) {
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

void onLostUser(SimpleOpenNI kinect, int userId)
{
  if (userId == calibratedUser) {
    println("USER LOST: USER ID -  " + userId);
    kinect.stopTrackingSkeleton(calibratedUser); // new
    isUserCalibrated = false;
    restartOK = true;
    println("RESTART OK");
  }
}

void keyPressed() {
  if (key == 'R'|| key == 'r') {
    restartOK = true;
    isUserCalibrated = false;
  }
}