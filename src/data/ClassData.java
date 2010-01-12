package com.intellij.rt.coverage.data;


import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.StringsPool;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ClassData implements CoverageData {
  private final String myClassName;

  private TIntObjectHashMap myLines = new TIntObjectHashMap(4, 0.99f);
  private LineData[] myLinesArray;
  
  private int myMaxLineNumber;

  private Map myStatus;

  private int[] myLineMask;

  public ClassData(final String name) {
    myClassName = name;
  }

  public String getName() {
    return myClassName;
  }

  public void save(final DataOutputStream os) throws IOException {
    CoverageIOUtil.writeINT(os, ProjectData.getDictValue(myClassName));
    if (myLineMask != null) {
      for (int i = 0; i < myLineMask.length; i++) {
        if (myLines.containsKey(i)) {
          final LineData lineData = (LineData) myLines.get(i);
          lineData.setHits(lineData.getHits() + myLineMask[i]);
        }
      }
      myLineMask = null;
    }
    if (myLines == null) {
      myLines = new TIntObjectHashMap();
      for(int line = 1; line < myLinesArray.length; line++) {
        final LineData data = myLinesArray[line];
        if (data != null) myLines.put(line, data);
      }
    }
    final Map sigLines = prepareSignaturesMap();
    final Set sigs = sigLines.keySet();
    CoverageIOUtil.writeINT(os, sigs.size());
    for (Iterator it = sigs.iterator(); it.hasNext();) {
      final String sig = (String)it.next();
      CoverageIOUtil.writeUTF(os, sig);
      final List lines = (List)sigLines.get(sig);
      CoverageIOUtil.writeINT(os, lines.size());
      for (int i = 0; i < lines.size(); i++) {
        ((LineData)lines.get(i)).save(os);
      }
    }
  }

  private Map prepareSignaturesMap() {
    final Map sigLines = new HashMap();
    final Object[] values = myLines.getValues();
    for (int i = 0; i < values.length; i++) {
      final LineData lineData = (LineData)values[i];
      final String sig = CoverageIOUtil.collapse(lineData.getMethodSignature());
      List lines = (List)sigLines.get(sig);
      if (lines == null) {
        lines = new ArrayList();
        sigLines.put(sig, lines);
      }
      lines.add(lineData);
    }
    return sigLines;
  }

  public void merge(final CoverageData data) {
    ClassData classData = (ClassData)data;
    for (TIntObjectIterator it = classData.myLines.iterator(); it.hasNext();) {
      it.advance();
      int key = it.key();
      final LineData mergedData = (LineData)classData.myLines.get(key);
      final LineData lineData = (LineData)myLines.get(key);
      if (lineData != null) {
        lineData.merge(mergedData);
      }
      else {
        final LineData createdLineData = getOrCreateLine(key, mergedData.getMethodSignature());
        registerMethodSignature(createdLineData);
        createdLineData.merge(mergedData);
      }
    }
  }

  public void touchLine(int line) {
    myLineMask[line]++;
  }

  public void initLineMask(int size) {
    myLineMask = new int[size + 1];
    Arrays.fill(myLineMask, 0);
  }

  public void touch(int line) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touch();
    }
  }

  public void touch(int line, int jump, boolean hit) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touchBrunch(jump, hit);
    }
  }

  public void touch(int line, int switchNumber, int key) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touchBrunch(switchNumber, key);
    }
  }

  public LineData getOrCreateLine(final int line, final String methodSig) {
    //create lines again if class was loaded again by another class loader; may be myLinesArray should be cleared
    if (myLines == null) myLines = new TIntObjectHashMap();
    LineData lineData = (LineData) myLines.get(line);
    if (lineData == null) {
      lineData = new LineData(line, StringsPool.getFromPool(methodSig));
      myLines.put(line, lineData);
    }
    if (line > myMaxLineNumber) myMaxLineNumber = line;
    return lineData;
  }

  public void registerMethodSignature(LineData lineData) {
    initStatusMap();
    myStatus.put(lineData.getMethodSignature(), null);
  }

  public void addLineJump(final int line, final int jump) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.addJump(jump);
    }
  }

  public void addLineSwitch(final int line, final int switchNumber, final int[] keys) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.addSwitch(switchNumber, keys);
    }
  }

  public void addLineSwitch(final int line, final int switchNumber, final int min, final int max) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.addSwitch(switchNumber, min, max);
    }
  }

  public LineData getLineData(int line) {
    if (myLines == null) return myLinesArray[line];
    return (LineData)myLines.get(line);
  }

  /** @noinspection UnusedDeclaration*/
  public Object[] getLines() {
    return myLines.getValues();
  }

  /** @noinspection UnusedDeclaration*/
  public boolean containsLine(int line) {
    return myLines.containsKey(line);
  }

  /** @noinspection UnusedDeclaration*/
  public Collection getMethodSigs() {
    initStatusMap();
    return myStatus.keySet();
  }

  private void initStatusMap() {
    if (myStatus == null) myStatus = new HashMap();
  }

  public Integer getStatus(String methodSignature) {
    Integer methodStatus = (Integer)myStatus.get(methodSignature);
    if (methodStatus == null) {
      final Object[] values = myLines.getValues();
      for (int i = 0; i < values.length; i++) {
        final LineData lineData = (LineData)values[i];
        if (lineData.getMethodSignature().equals(methodSignature)) {
          if (lineData.getStatus() != LineCoverage.NONE) {
            methodStatus = new Integer(LineCoverage.PARTIAL);
            break;
          }
        }
      }
      if (methodStatus == null) methodStatus = new Integer(LineCoverage.NONE);
      myStatus.put(methodSignature, methodStatus);
    }
    return methodStatus;
  }

  public void removeLine(final int line) {
    myLines.remove(line);
  }

  public void fillArray() {
    myLinesArray = new LineData[myMaxLineNumber + 1];
    for(int line = 1; line <= myMaxLineNumber; line++) {
      final LineData lineData = (LineData)myLines.get(line);
      if (lineData != null) {
        lineData.fillArrays();
      }
      myLinesArray[line] = lineData;
    }
    myLines = null;
  }

  public String toString() {
    return myClassName;
  }
}
