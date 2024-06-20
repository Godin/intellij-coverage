/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.lines;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Ignore lines generated by compiler for try-with-resources block.
 *
 * @see TryWithResourcesLineFilter.State
 */
public class TryWithResourcesLineFilter extends CoverageFilter {
  private State myState = State.INITIAL;
  private boolean myHasInstructions = false;
  private int myCurrentLine = -1;
  private int myJumpsToRemove = 0;

  // INITIAL → STORE_INITIAL_EXCEPTION <--------|
  //    ↘         ↓                             |
  //     LOAD_RESOURCE ↔ CHECK_RESOURCE_NULL    |
  //          ↕                                 |
  //       CALL_CLOSE                           |
  //          ↓                                 |
  //         GOTO                               |
  //          ↓                                 |
  //  STORE_ADDITIONAL_EXCEPTION                |
  //          ↓                                 |
  //  LOAD_INITIAL_EXCEPTION                    |
  //          ↓                                 |
  //  LOAD_ADDITIONAL_EXCEPTION                 |
  //          ↓                                 |
  //  CALL_ADD_SUPPRESSED     ↘                 |
  //          ↓                 GOTO_2          |
  //  LOAD_INITIAL_EXCEPTION_2 ↙                |
  //      ↕             ↘                       |
  // CALL_CLOSE_2       THROW ------------------|
  //    ↓
  //   GOTO_3
  private enum State {
    INITIAL,
    STORE_INITIAL_EXCEPTION,
    LOAD_RESOURCE,
    CHECK_RESOURCE_NULL,
    CALL_CLOSE, // final
    GOTO, // final
    STORE_ADDITIONAL_EXCEPTION,
    LOAD_INITIAL_EXCEPTION,
    LOAD_ADDITIONAL_EXCEPTION,
    CALL_ADD_SUPPRESSED,
    GOTO_2, // java 8
    LOAD_INITIAL_EXCEPTION_2,
    THROW, // final
    CALL_CLOSE_2, // java 8
    GOTO_3, // java 8
  }

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return true;
  }

  private void tryRemoveLine() {
    if (myCurrentLine != -1 && !myHasInstructions
        && (myState == State.GOTO || myState == State.THROW || myState == State.CALL_CLOSE || myState == State.GOTO_3)) {
      myContext.removeLine(myCurrentLine);
      myCurrentLine = -1;
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    tryRemoveLine();
    super.visitLineNumber(line, start);
    myCurrentLine = line;
    myState = State.INITIAL;
    myHasInstructions = false;
    myJumpsToRemove = 0;
  }

  @Override
  public void visitEnd() {
    tryRemoveLine();
    super.visitEnd();
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    if ((myState == State.INITIAL || myState == State.THROW) && opcode == Opcodes.ASTORE) {
      myState = State.STORE_INITIAL_EXCEPTION;
    } else if (opcode == Opcodes.ALOAD
        && (myState == State.STORE_INITIAL_EXCEPTION || myState == State.CHECK_RESOURCE_NULL
        || myState == State.INITIAL || myState == State.CALL_CLOSE)) {
      myState = State.LOAD_RESOURCE;
    } else if (myState == State.GOTO && opcode == Opcodes.ASTORE) {
      myState = State.STORE_ADDITIONAL_EXCEPTION;
    } else if (myState == State.STORE_ADDITIONAL_EXCEPTION && opcode == Opcodes.ALOAD) {
      myState = State.LOAD_INITIAL_EXCEPTION;
    } else if (myState == State.LOAD_INITIAL_EXCEPTION && opcode == Opcodes.ALOAD) {
      myState = State.LOAD_ADDITIONAL_EXCEPTION;
    } else if ((myState == State.CALL_ADD_SUPPRESSED || myState == State.GOTO_2 || myState == State.CALL_CLOSE_2)
        && opcode == Opcodes.ALOAD) {
      myState = State.LOAD_INITIAL_EXCEPTION_2;
    } else if (myState == State.CALL_CLOSE_2 && opcode == Opcodes.GOTO) {
      myState = State.GOTO_3;
    } else {
      myState = State.INITIAL;
      myHasInstructions = true;
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (myState == State.LOAD_RESOURCE && opcode == Opcodes.IFNULL) {
      myState = State.CHECK_RESOURCE_NULL;
      myJumpsToRemove++;
    } else if (myState == State.CALL_CLOSE && opcode == Opcodes.GOTO) {
      myState = State.GOTO;
      while (myJumpsToRemove-- > 0) {
        myContext.removeLastJump();
      }
    } else if (myState == State.CALL_ADD_SUPPRESSED && opcode == Opcodes.GOTO) {
      myState = State.GOTO_2;
    } else {
      myState = State.INITIAL;
      myHasInstructions = true;
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if ((opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL)
        && "close".equals(name)
        && "()V".equals(descriptor)) {
      if (myState == State.LOAD_RESOURCE) {
        myState = State.CALL_CLOSE;
        return;
      } else if (myState == State.LOAD_INITIAL_EXCEPTION_2) {
        myState = State.CALL_CLOSE_2;
        return;
      }
    } else if (myState == State.LOAD_ADDITIONAL_EXCEPTION
        && opcode == Opcodes.INVOKEVIRTUAL
        && "java/lang/Throwable".equals(owner)
        && "addSuppressed".equals(name)
        && "(Ljava/lang/Throwable;)V".equals(descriptor)) {
      myState = State.CALL_ADD_SUPPRESSED;
      return;
    }
    myState = State.INITIAL;
    myHasInstructions = true;
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (myState == State.LOAD_INITIAL_EXCEPTION_2 && opcode == Opcodes.ATHROW) {
      myState = State.THROW;
    } else {
      myState = State.INITIAL;
      myHasInstructions = true;
    }
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    myHasInstructions = true;
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    myHasInstructions = true;
  }

  @Override
  public void visitIincInsn(int varIndex, int increment) {
    super.visitIincInsn(varIndex, increment);
    myHasInstructions = true;
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    myHasInstructions = true;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    myHasInstructions = true;
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    myHasInstructions = true;
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    myHasInstructions = true;
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    myHasInstructions = true;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    myHasInstructions = true;
  }
}
