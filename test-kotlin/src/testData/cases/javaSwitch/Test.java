/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package testData.cases.javaSwitch;

// instructions & branches

public class Test { // coverage: FULL // stats: 2/2

  void singleBranchSwitch1(int x) {
    switch (x) { // coverage: PARTIAL // stats: 2/2 1/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
        break; // coverage: FULL // stats: 1/1
      }
      case 2: {
        System.out.println("Case 2"); // coverage: NONE // stats: 0/3
        break;
      }
    }
  }

  void singleBranchSwitch2(int x) {
    switch (x) { // coverage: PARTIAL // stats: 2/2 1/2
      case 1: {
        System.out.println("Case 1"); // coverage: NONE // stats: 0/3
        break; // coverage: NONE // stats: 0/1
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void defaultBranchSwitch(int x) {
    switch (x) { // coverage: PARTIAL // stats: 2/2 0/2
      case 1: {
        System.out.println("Case 1"); // coverage: NONE // stats: 0/3
        break; // coverage: NONE // stats: 0/1
      }
      case 2: {
        System.out.println("Case 2"); // coverage: NONE // stats: 0/3
        break; // coverage: NONE // stats: 0/1
      }
      default: {
        System.out.println("Default"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void fullyCoveredSwitch(int x) {
    switch (x) { // coverage: PARTIAL // stats: 2/2 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
        break; // coverage: FULL // stats: 1/1
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void fullyCoveredSwitchWithDefault(int x) {
    switch (x) { // coverage: FULL // stats: 2/2 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
        break; // coverage: FULL // stats: 1/1
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break; // coverage: FULL // stats: 1/1
      }
      default: {
        System.out.println("Default"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void fullyCoveredSwitchWithoutDefault(int x) {
    switch (x) { // coverage: PARTIAL // stats: 2/2 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
        break; // coverage: FULL // stats: 1/1
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break; // coverage: FULL // stats: 1/1
      }
      default: {
        System.out.println("Default"); // coverage: NONE // stats: 0/3
        break;
      }
    }
  }

  void switchWithFallThrough(int x) {
    switch (x) { // coverage: PARTIAL // stats: 2/2 1/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  public static void main(String[] args) {
    Test switches = new Test(); // coverage: FULL // stats: 4/4

    switches.singleBranchSwitch1(1); // coverage: FULL // stats: 3/3
    switches.singleBranchSwitch2(2); // coverage: FULL // stats: 3/3
    switches.defaultBranchSwitch(3); // coverage: FULL // stats: 3/3

    switches.fullyCoveredSwitch(1); // coverage: FULL // stats: 3/3
    switches.fullyCoveredSwitch(2); // coverage: FULL // stats: 3/3

    switches.fullyCoveredSwitchWithDefault(1); // coverage: FULL // stats: 3/3
    switches.fullyCoveredSwitchWithDefault(2); // coverage: FULL // stats: 3/3
    switches.fullyCoveredSwitchWithDefault(3); // coverage: FULL // stats: 3/3

    switches.fullyCoveredSwitchWithoutDefault(1); // coverage: FULL // stats: 3/3
    switches.fullyCoveredSwitchWithoutDefault(2); // coverage: FULL // stats: 3/3

    switches.switchWithFallThrough(1); // coverage: FULL // stats: 3/3
  }
}
