/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import implementation.AllMyLatches.*;
import utilitytypes.EnumComparison;
import utilitytypes.EnumOpcode;
import baseclasses.InstructionBase;
import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import utilitytypes.Operand;
import voidtypes.VoidLatch;
import baseclasses.CpuCore;
import voidtypes.VoidInstruction;

import java.lang.reflect.Array;

/**
 * The AllMyStages class merely collects together all of the pipeline stage 
 * classes into one place.  You are free to split them out into top-level
 * classes.
 * 
 * Each inner class here implements the logic for a pipeline stage.
 * 
 * It is recommended that the compute methods be idempotent.  This means
 * that if compute is called multiple times in a clock cycle, it should
 * compute the same output for the same input.
 * 
 * How might we make updating the program counter idempotent?
 * 
 * @author
 */
public class AllMyStages {
    /*** Fetch Stage ***/
    static class Fetch extends PipelineStageBase<VoidLatch,FetchToDecode> {
        public Fetch(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        // Does this state have an instruction it wants to send to the next
        // stage?  Note that this is computed only for display and debugging
        // purposes.
        boolean has_work;

        /**
         * For Fetch, this method only has diagnostic value.  However,
         * stageHasWorkToDo is very important for other stages.
         *
         * @return Status of Fetch, indicating that it has fetched an
         *         instruction that needs to be sent to Decode.
         */
        @Override
        public boolean stageHasWorkToDo() {
            return has_work;
        }

        /**
         * For other stages, status can be automatically computed, but
         * for Fetch, we have to compute it.  This method has only diagnostic
         * value.
         *
         * @return Status string
         */
        @Override
        public String getStatus() {
            // Generate a string that helps you debug.
            GlobalData globals = (GlobalData)core.getGlobalResources();
            String s = super.getStatus();
            if (globals.current_branch_state == GlobalData.EnumBranchState.WAITING) {
                if (s.length() > 0) s += ", ";
                s += "ResolveWait";
            }
            return s;
        }

        @Override
        public void compute(VoidLatch input, FetchToDecode output) {
            GlobalData globals = (GlobalData)core.getGlobalResources();
            int pc = globals.program_counter;
            // Fetch the instruction
            InstructionBase ins = globals.program.getInstructionAt(pc);
            // Initialize this status flag to assume a stall or bubble condition
            // by default.
            has_work = false;

            // If the instruction is NULL (like we ran off the end of the
            // program), just return.  However, for diagnostic purposes,
            // we make sure something meaningful appears when
            // CpuSimulator.printStagesEveryCycle is set to true.
            if (ins.isNull()) {
                // Fetch is working on no instruction at no address
                setActivity("----: NULL");
                // Nothing more to do.
                return;
            }

            // Also don't do anything if we're stalled waiting on branch
            // resolution.
            if (globals.current_branch_state != GlobalData.EnumBranchState.NULL) {
                // Fetch is waiting on branch resolution
                setActivity("----: BRANCH-BUBBLE");
                // Since we're stalled, nothing more to do.
                return;
            }
            // Compute the value of the next program counter, to be committed
            // in advanceClock depending on stall states.  This makes
            // computing the next PC idempotent.
            globals.next_program_counter_nobranch = pc + 1;

            // Since there is no input pipeline register, we have to inform
            // the diagnostic helper code explicitly what instruction Fetch
            // is working on.
            has_work = true;
            setActivity(ins.toString());

            // If the instruction is a branch, request that the branch wait
            // state be set.  This will be committed in Fetch.advanceClock
            // if Decode isn't stalled.  This too is idempotent.
            if (ins.getOpcode().isBranch()) {
                globals.next_branch_state_fetch = GlobalData.EnumBranchState.WAITING;
            }

            // Send the fetched instruction to the output pipeline register.
            // PipelineRegister.advanceClock will ignore this if
            // Decode is stalled, and Fetch.compute will keep setting the
            // output instruction to the same thing over and over again.
            // In the stall case Fetch.advanceClock will not change the program
            // counter, nor will it commit globals.next_branch_state_fetch to
            // globals.branch_state_fetch.
            output.setInstruction(ins);
           // if (ins.isNull()) return;


            // Do something idempotent to compute the next program counter.
            
            // Don't forget branches, which MUST be resolved in the Decode
            // stage.  You will make use of global resources to commmunicate
            // between stages.
            
            // Your code goes here...
            
            output.setInstruction(ins);
        }
        
        @Override
       /* public boolean stageWaitingOnResource() {
            // Hint:  You will need to implement this for when branches
            // are being resolved.
            return false;
        }
        */
        
        /**
         * This function is to advance state to the next clock cycle and
         * can be applied to any data that must be updated but which is
         * not stored in a pipeline register.
         */
       /* @Override

        public void advanceClock() {
            // Hint:  You will need to implement this help with waiting
            // for branch resolution and updating the program counter.
            // Don't forget to check for stall conditions, such as when
            // nextStageCanAcceptWork() returns false.
            if (nextStageCanAcceptWork()){
                GlobalData globals = (GlobalData)core.getGlobalResources();
                if (!globals.waitingBra){
                    globals.program_counter++;
                }
            }
        }
    }
*/
        public void advanceClock() {
            // Only take take action if Decode is able to accept a new
            // instruction.
            if (nextStageCanAcceptWork()) {
                GlobalData globals = (GlobalData)core.getGlobalResources();

                if (globals.current_branch_state == GlobalData.EnumBranchState.WAITING) {
                    // If we're currently waiting for a branch resolution...

                    // See if the Decode stage has provided a resolution
                    if (globals.next_branch_state_decode != GlobalData.EnumBranchState.NULL) {

                        // Take action based on the resolution.
                        switch (globals.next_branch_state_decode) {

                            // If Decode resolves that the branch is to be taken...
                            case TAKEN:
                                // Set the PC to the branch target
                                globals.program_counter = globals.next_program_counter_takenbranch;
                                break;

                            // If Decode resolves that the branch is no to be taken...
                            case NOT_TAKEN:
                                // Set the PC to the address immediately after the branch
                                globals.program_counter = globals.next_program_counter_nobranch;
                                break;

                        }

                        // Clear the signal from Decode
                        globals.next_branch_state_decode = GlobalData.EnumBranchState.NULL;

                        // Clear the stall state for Fetch
                        globals.current_branch_state = GlobalData.EnumBranchState.NULL;
                    }
                } else {
                    // If we've not been waiting on a branch resolution...

                    if (globals.next_branch_state_fetch != GlobalData.EnumBranchState.NULL) {
                        // If Fetch wants to change its stall state...

                        // Commit the new state
                        globals.current_branch_state = globals.next_branch_state_fetch;

                        // Clear the signal to change the state
                        globals.next_branch_state_fetch = GlobalData.EnumBranchState.NULL;
                    } else {
                        // If Fetch is not switching to a stall state,
                        // increment the program counter.
                        globals.program_counter = globals.next_program_counter_nobranch;
                    }
                }
            }
        }
    }



    /*** Decode Stage ***/
    static class Decode extends PipelineStageBase<FetchToDecode,DecodeToExecute> {
        public Decode(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }
       // public boolean waitingResource;
        boolean invalid_register = false;
        @Override
        public boolean stageWaitingOnResource() {
            // Hint:  You will need to implement this to deal with 
            // dependencies.
           // if (waitingResource) return true;
//            GlobalData globals = (GlobalData)core.getGlobalResources();

            //return false;
            return invalid_register;
        }
        // Destination register to mark invalid if the next stage is able
        // to accept new work.  A value of -1 means "none."
        int register_to_invalidate = -1;


        @Override
        public void compute(FetchToDecode input, DecodeToExecute output) {
            InstructionBase ins = input.getInstruction();
            
            // You're going to want to do something like this:
            
            // VVVVV LOOK AT THIS VVVVV
            ins = ins.duplicate();
            // ^^^^^ LOOK AT THIS ^^^^^
            
            // The above will allow you to do things like look up register 
            // values for operands in the instruction and set them but avoid 
            // altering the input latch if you're in a stall condition.
            // The point is that every time you enter this method, you want
            // the instruction and other contents of the input latch to be
            // in their original state, unaffected by whatever you did 
            // in this method when there was a stall condition.
            // By cloning the instruction, you can alter it however you
            // want, and if this stage is stalled, the duplicate gets thrown
            // away without affecting the original.  This helps with 
            // idempotency.
            invalid_register = false;
            // No register to invalidate.
            register_to_invalidate = -1;


            // These null instruction checks are mostly just to speed up
            // the simulation.  The Void types were created so that null
            // checks can be almost completely avoided.
            if (ins.isNull()) return;

            // Get the opcode and determine if oper0 is a source
            EnumOpcode opcode = ins.getOpcode();
            boolean oper0src = opcode.oper0IsSource();

            // Get the register file and valid flags
            GlobalData globals = (GlobalData)core.getGlobalResources();
            int[] regfile = globals.register_file;
            boolean[] reginval = globals.register_invalid;

            Operand oper0 = ins.getOper0();
            Operand src1  = ins.getSrc1();
            Operand src2  = ins.getSrc2();


            output.forwarding_value[0] = 0;
            output.forwarding_value[1] = 0;
            output.forwarding_value[2] = 0;
            output.forward0 = output.forward1 = output.forward2 = false;
            for (int i=1;i<4;i++){
                int regnum = core.getForwardingDestinationRegisterNumber(i);
                if (regnum < 0) {
                    //System.out.println(latchtypename + " has no target register");
                } else {
                    boolean valid = core.isForwardingResultValid(i);
                    if (valid) {
                        int value = core.getForwardingResultValue(i);
                        if (oper0.getRegisterNumber() == regnum){
                            output.forwarding_value[0] = value;
                            output.forward0 = true;
                        }else
                        if (src1.getRegisterNumber() == regnum){
                            output.forwarding_value[1] = value;
                            output.forward1 = true;
                        }else
                        if (src2.getRegisterNumber() == regnum){
                            output.forwarding_value[2] = value;
                            output.forward2 = true;
                        }

                    } else {

                    }
                }
            }


            int regnum0 = oper0.getRegisterNumber();
            if (oper0src) {
                // If oper0 is a source, check if it's register is invalid.
                if (oper0.isRegister()&&(output.forward0 == false)) {
                    if (reginval[regnum0]) {
                        invalid_register = true;
                        // If there's a stall, no point in doing anything else,
                        // do might as well just bail out.
                        return;
                    } else {
                        // If the register is valid, look up its value.
                        oper0.lookUpFromRegisterFile(regfile);
                        // This is idempotent because setting of operand
                        // values is being done to the duplicate of the
                        // instruction.
                    }
                }
                // Get the value in case we need it
            }

            // Check src1 for stall; look up if valid.
            if (src1.isRegister()&&(output.forward1 == false)) {
                if (reginval[src1.getRegisterNumber()]) {
                    invalid_register = true;
                    // Nothing further to do if stalled.
                    return;
                } else {
                    src1.lookUpFromRegisterFile(regfile);
                }
            }

            // Check src2 for stall; look up if valid.
            if (src2.isRegister()&&(output.forward2 == false)) {
                if (reginval[src2.getRegisterNumber()]) {
                    invalid_register = true;
                    // Nothing further to do if stalled.
                    return;
                } else {
                    src2.lookUpFromRegisterFile(regfile);
                }
            }
            int value0 = oper0.getValue();
            int value1 = src1.getValue();
            int value2 = src2.getValue();
            if (output.forward0) value0 = output.forwarding_value[0];
            if (output.forward1) value1 = output.forwarding_value[1];
            if (output.forward2) value2 = output.forwarding_value[2];


            // ** SECTION 3:  EVALUATE BRANCH CONDITIONS
            // Process branch instructions and send signals to
            // Fetch about branch resolution.

            // This code is idempotent because it is setting signals
            // (next_program_counter_takenbranch, next_branch_state_decode)
            // that don't take effect except in some pipeline stage's
            // advanceClock method, which is able to check
            // nextStageCanAcceptWork before committing anything.  In this
            // case, it's Fetch.nextStageCanAcceptWork, but that's okay.
            // The point is that if for any reason this stage couldn't
            // move forward (hand off its work to the next stage),
            // then there would be no side-effects, and this code would
            // produce exactly the same effects on the next cycle.

            boolean take_branch = false;
            InstructionBase null_ins = VoidInstruction.getVoidInstruction();

            switch (opcode) {
                case BRA:
                    // The CMP instruction just sets its destination to
                    // (src1-src2).  The result of that is in oper0 for the
                    // BRA instruction.  See comment in MyALU.java.
                    switch (ins.getComparison()) {
                        case EQ:
                            take_branch = (value0 == 0);
                            break;
                        case NE:
                            take_branch = (value0 != 0);
                            break;
                        case GT:
                            take_branch = (value0 > 0);
                            break;
                        case GE:
                            take_branch = (value0 >= 0);
                            break;
                        case LT:
                            take_branch = (value0 < 0);
                            break;
                        case LE:
                            take_branch = (value0 <= 0);
                            break;
                    }

                    if (take_branch) {
                        // If the branch is taken, send a signal to Fetch
                        // that specifies the branch target address, via
                        // "globals.next_program_counter_takenbranch".
                        // If the label is valid, then use its address.
                        // Otherwise, the target address will be found in
                        // src1.
                        if (ins.getLabelTarget().isNull()) {
                            globals.next_program_counter_takenbranch = value1;
                        } else {
                            globals.next_program_counter_takenbranch =
                                    ins.getLabelTarget().getAddress();
                        }

                        // Send a signal to Fetch, indicating that the branch
                        // is resolved taken.  This will be picked up by
                        // Fetch.advanceClock on the same clock cycle.
                        globals.next_branch_state_decode = GlobalData.EnumBranchState.TAKEN;
                    } else {
                        // Send a signal to Fetch, indicating that the branch
                        // is resolved not taken.
                        globals.next_branch_state_decode = GlobalData.EnumBranchState.NOT_TAKEN;
                    }

                    output.setInstruction(null_ins);
                    // All done; return.
                    return;

                case JMP:
                    // JMP is an inconditionally taken branch.  If the
                    // label is valid, then take its address.  Otherwise
                    // its operand0 contains the target address.
                    if (ins.getLabelTarget().isNull()) {
                        globals.next_program_counter_takenbranch = value0;
                    } else {
                        globals.next_program_counter_takenbranch =
                                ins.getLabelTarget().getAddress();
                    }

                    // Send a signal to Fetch, indicating that the branch is
                    // taken.
                    globals.next_branch_state_decode = GlobalData.EnumBranchState.TAKEN;

                    // Replace the JMP with a bubble for later stages.
                    output.setInstruction(null_ins);
                    // All done; return.
                    return;

                case CALL:
                    // Not implemented yet
                    return;
            }

            if (opcode.needsWriteback()) {
                register_to_invalidate = regnum0;
            }

            // For all other instructions, set instruction in the output
            // pipeline register.
            output.setInstruction(ins);
        }


        @Override
        public void advanceClock() {
            // Only take take action if the next stage is able to accept a new
            // instruction.
            if (nextStageCanAcceptWork() && register_to_invalidate>=0) {
                GlobalData globals = (GlobalData)core.getGlobalResources();
                boolean[] reginval = globals.register_invalid;

                // Invalidate the requested register
                reginval[register_to_invalidate] = true;
                // Clear the signal
                register_to_invalidate = -1;

            }
        }
    }




    /*GlobalData globals = (GlobalData)core.getGlobalResources();
            int[] regfile = globals.register_file;
            boolean[] register_invalid = globals.register_invalid;

            // Do what the decode stage does:
            // - Look up source operands
            // - Decode instruction
            // - Resolve branches
            if (globals.waitingBra) return;

            globals.waitingBra = true;


            EnumOpcode opcode = ins.getOpcode();

            Operand oper0 = ins.getOper0();
            Operand sr1 = ins.getSrc1();
            Operand sr2 = ins.getSrc2();

            waitingResource = false;
            if (oper0.isRegister()){
                if (register_invalid[oper0.getRegisterNumber()]){
                    waitingResource = true;
                }
            }
            if (sr1.isRegister()){
                if (register_invalid[sr1.getRegisterNumber()]){
                    waitingResource = true;
                }
            }
            if (sr2.isRegister()){
                if (register_invalid[sr2.getRegisterNumber()]){
                    waitingResource = true;
                }
            }

            if (waitingResource) return;



            if (oper0.isRegister()){
                //register_invalid[oper0.getRegisterNumber()] = true;
                oper0.lookUpFromRegisterFile(regfile);
            }
            if (sr1.isRegister()){
                sr1.lookUpFromRegisterFile(regfile);
            }
            if (sr2.isRegister()){
                sr2.lookUpFromRegisterFile(regfile);
            }

            if (opcode.isBranch()){

                if (opcode == EnumOpcode.BRA){
                    EnumComparison cmp = ins.getComparison();
                    int value = oper0.getValue();
                    if (value<0){
                        if (cmp == EnumComparison.LT || cmp == EnumComparison.LE){
                            globals.program_counter = ins.getLabelTarget().getAddress();
                        }
                    }else if (value >0){
                        if (cmp == EnumComparison.GT || cmp == EnumComparison.GE){
                            globals.program_counter = ins.getLabelTarget().getAddress();
                        }
                    }else {
                        if (cmp == EnumComparison.EQ || cmp == EnumComparison.LE || cmp == EnumComparison.GE){
                            globals.program_counter = ins.getLabelTarget().getAddress();
                        }
                    }

                }else if (opcode == EnumOpcode.JMP){
                    globals.program_counter = ins.getLabelTarget().getAddress();
                }

                //globals.waitingBra = false;

            }
            output.setInstruction(ins);
            // Set other data that's passed to the next stage.
        }
    }
    */

    /*** Execute Stage ***/
    static class Execute extends PipelineStageBase<DecodeToExecute,ExecuteToMemory> {
        public Execute(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public void compute(DecodeToExecute input, ExecuteToMemory output) {
            InstructionBase ins = input.getInstruction();
            if (ins.isNull()) return;
            int[] forwarding = input.forwarding_value;

                if (input.forward0) {
                    ins.getOper0().setValue(forwarding[0]);
                }
                if (input.forward1) {
                    ins.getSrc1().setValue(forwarding[1]);
                }
                if (input.forward2) {
                    ins.getSrc2().setValue(forwarding[2]);
                }



            int source1 = ins.getSrc1().getValue();
            int source2 = ins.getSrc2().getValue();
            int oper0 =   ins.getOper0().getValue();

            int result = MyALU.execute(ins.getOpcode(), source1, source2, oper0);
                        
            // Fill output with what passes to Memory stage...
            output.setInstruction(ins);
            output.result = result;
            // Set other data that's passed to the next stage.
        }
    }
    

    /*** Memory Stage ***/
    static class Memory extends PipelineStageBase<ExecuteToMemory,MemoryToWriteback> {
        public Memory(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public void compute(ExecuteToMemory input, MemoryToWriteback output) {
            InstructionBase ins = input.getInstruction();
            if (ins.isNull()) return;

            // Access memory...
            //int result = input.result;
            int result;
            int address = input.result;
            GlobalData globals = (GlobalData)core.getGlobalResources();

            EnumOpcode opcode = ins.getOpcode();

            Operand oper0 = ins.getOper0();
            if (opcode == EnumOpcode.LOAD){
                result = globals.memory_file[address];
                output.result = result;
            }else if (opcode == EnumOpcode.STORE){
                globals.memory_file[address] = ins.getOper0().getValue();
            }else{
                output.result = input.result;
            }


            output.setInstruction(ins);
           // output.result = result;
            // Set other data that's passed to the next stage.
        }
    }
    

    /*** Writeback Stage ***/
    static class Writeback extends PipelineStageBase<MemoryToWriteback,VoidLatch> {
        public Writeback(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public void compute(MemoryToWriteback input, VoidLatch output) {
            InstructionBase ins = input.getInstruction();
            if (ins.isNull()) return;

            // Write back result to register file
            EnumOpcode opcode = ins.getOpcode();
            Operand oper0 = ins.getOper0();
            Operand sr1 = ins.getSrc1();
            Operand sr2 = ins.getSrc2();
            GlobalData globals = (GlobalData)core.getGlobalResources();

            if (opcode.needsWriteback()){
                oper0.setValue(input.result);
                if (oper0.isRegister()){
                    globals.register_file[ins.getOper0().getRegisterNumber()] = input.result;
                    globals.register_invalid[ins.getOper0().getRegisterNumber()] = false;
                }
            }

            if (input.getInstruction().getOpcode() == EnumOpcode.HALT) {
                // Stop the simulation
               // globals.shouldStop = true;
                globals.running = false;
            }

            //if (opcode.isBranch()){
                //globals.waitingBra = false;
            //}

        }
    }
}
