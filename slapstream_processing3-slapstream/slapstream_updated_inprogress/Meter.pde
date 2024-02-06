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
  
  void display() {
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
  
  void updatePower(float powerValue) {
    powerValue = map(powerValue, 0, 80, 0, h);
    if (powerValue > h) powerValue = h;
    fill(0,255,0);
    rect(x,y+h,w,-powerValue);
  }
  
}