package edu.cmu.lti.f12.hw2.hw2_team08.passage;

public class PassageOffsetScore implements KeytermWindowScorer {
  @Override
  public double scoreWindow(int begin, int end, int matchesFound,
      int totalMatches, int keytermsFound, int totalKeyterms,
      int textSize) {
    return ( (double)textSize - (double)begin ) / (double)textSize;
  }
}
