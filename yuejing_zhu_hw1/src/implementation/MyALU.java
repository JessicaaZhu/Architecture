/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import utilitytypes.EnumOpcode;

/**
 * The code that implements the ALU has been separates out into a static
 * method in its own class.  However, this is just a design choice, and you
 * are not required to do this.
 * 
 * @author 
 */
public class MyALU {
    static int execute(EnumOpcode opcode, int input1, int input2, int oper0) {
        int result = 0;
        
        // Implement code here that performs appropriate computations for
        // any instruction that requires an ALU operation.  See
        // EnumOpcode.
        switch (opcode){
            case MOVC:
                result = input1;
                break;
            case ADD:
                result = input1+input2;
                break;
            case SUB:
                result = input1-input2;
                break;
            case CMP:
                result = input1 - input2;
                break;
            case STORE:
            case LOAD:
                result = input1 + input2;
                break;
            case OUT:
                result = oper0;
                System.out.println("@@out:"+result);
                break;
                default:
                    break;
        }


        
        return result;
    }    
}
