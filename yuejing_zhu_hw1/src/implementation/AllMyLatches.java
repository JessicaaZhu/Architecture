/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.InstructionBase;
import baseclasses.LatchBase;
import utilitytypes.EnumOpcode;
import utilitytypes.Operand;

/**
 * Definitions of latch contents for pipeline registers.  Pipeline registers
 * create instances of these for passing data between pipeline stages.
 *
 * AllMyLatches is merely to collect all of these classes into one place.
 * It is not necessary for you to do it this way.
 *
 * You must fill in each latch type with the kind of data that passes between
 * pipeline stages.
 *
 * @author
 */
public class AllMyLatches {
    public static class FetchToDecode extends LatchBase {
        // LatchBase already includes a field for the instruction.
    }

    public static class DecodeToExecute extends LatchBase {
        // LatchBase already includes a field for the instruction.
        // What else do you need here?
        //- For DecodeToExecute, all instructions that will do a writeback
        //     *   (except LOAD) will have a valid result in ExecuteToMemory on the
        //     *   next cycle..
        public boolean isForwardingResultValidNextCycle() {
            InstructionBase ins = this.getInstruction();
            EnumOpcode opcode = ins.getOpcode();
            if (opcode.needsWriteback()&&opcode != EnumOpcode.LOAD){
                return true;
            }
            return false;
        }
        public int[] forwarding_value = new  int[3];
        public boolean forward0;
        public boolean forward1;
        public boolean forward2;


    }

    public static class ExecuteToMemory extends LatchBase {
        // LatchBase already includes a field for the instruction.
        // What do you need here?
        public int result;
        public boolean isForwardingResultValid() {
            InstructionBase ins = this.getInstruction();
            EnumOpcode opcode = ins.getOpcode();
            if (opcode.needsWriteback()&&opcode != EnumOpcode.LOAD){
                return true;
            }
            return false;
        }
        //- For ExecuteToMemory, all instructions that will do writeback will
        //     *   have a valid result in MemoryToWriteback on the next cycle;
        public boolean isForwardingResultValidNextCycle() {
            InstructionBase ins = this.getInstruction();
            EnumOpcode opcode = ins.getOpcode();
            if (opcode.needsWriteback()){
                return true;
            }
            return false;
        }
        /**
         * You must override this method if it ever needs to return a value.
         *
         * If there is a target register in the instruction, return the computed
         * result value.  Otherwise, it doesn't matter what you return.
         *
         * @return Result value that will be written to target register.
         */
        public int getForwardingResultValue() {

            return this.result;
        }
    }

    public static class MemoryToWriteback extends LatchBase {
        // LatchBase already includes a field for the instruction.
        // What do you need here?
        public int result;
        public boolean isForwardingResultValid() {
            InstructionBase ins = this.getInstruction();
            EnumOpcode opcode = ins.getOpcode();
            if (opcode.needsWriteback()){
                return true;
            }
            return false;
        }
        /**
         * You must override this method if it ever needs to return a value.
         *
         * If there is a target register in the instruction, return the computed
         * result value.  Otherwise, it doesn't matter what you return.
         *
         * @return Result value that will be written to target register.
         */
        public int getForwardingResultValue() {

            return this.result;
        }

    }
}
