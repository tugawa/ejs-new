/*
   CodeGenerator.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Takafumi Kataoka, 2017-18
     Tomoya Nonaka, 2017-18
     Tomoharu Ugawa, 2017-18
     Hideya Iwasaki, 2017-18

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/
package ejsc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class CodeGenerator extends IASTBaseVisitor {

    static class JSLabel {
        String name;
        Label label;

        JSLabel(String name, Label label) {
            this.name = name;
            this.label = label;
        }
    }

    static class Environment {

        // Frame is made for each IASTFunction.
        static class Frame {
			List<String> params;
			List<String> argumentsVars;
            List<String> staticLocals;
            HashMap<String, Register> regHash;
            LinkedList<String> dynamicLocals;
            boolean hasFrame;

            private int maxNumOfDynamicLocals = 0;
            private int numOfRegisters = 0;
            private final static int MAX_OF_STACK_OF_USING_REGISTERS = 256;

            private int numOfArgumentRegisters = 0;
            private List<Register> argumentRegisters = new LinkedList<Register>();
            private Register[] paramRegisters;

            LinkedList<JSLabel> breakLabels = new LinkedList<JSLabel>();
            LinkedList<JSLabel> continueLabels = new LinkedList<JSLabel>();

            Register registerOfGlobalObj = null;

			Frame(List<String> params, List<String> argumentsVars, List<String> frameVars, List<String> regVars, boolean hasFrame) {
				numOfRegisters = 1 + params.size(); // this + params
            	paramRegisters = new Register[numOfRegisters];
				regHash = new HashMap<String, Register>();
				paramRegisters[0] = new Register(1); // this
				for (int i = 0; i < params.size(); i++) {
					Register r = new Register(i + 2);
					String var = params.get(i);
					if (regVars.contains(var))
						regHash.put(var, r);
					paramRegisters[i + 1] = r;
				}
				for (String reg : regVars) {
					if (!params.contains(reg))
						regHash.put(reg, freshRegister());
				}
				this.params = params;
				this.argumentsVars = argumentsVars; // created by FunctionDeclaration
				this.staticLocals = frameVars; // created by VariableDeclaration and FunctionDeclaration
                this.dynamicLocals = new LinkedList<String>(); // created by CatchClause and WithStatement
                this.hasFrame = hasFrame;
            }

            void close() {
                for (int i = 0; i < numOfArgumentRegisters; i++) {
                    argumentRegisters.get(i).n = numOfRegisters + numOfArgumentRegisters - i;
                }
            }

            // If you need a new LocalVar while compiling IASTFunction's body,
            // call the method at the beginning scope of the LocalVar.
            void addLocal(String s) {
                dynamicLocals.addFirst(s);
                if (dynamicLocals.size() >  maxNumOfDynamicLocals) {
                    maxNumOfDynamicLocals = dynamicLocals.size();
                }
            }

            // You must call this method
            // at the ending scope of a added LocalVar by "addLocal" method.
            void removeLocal() {
                dynamicLocals.removeFirst();
            }
            
            void pushBreakLabel(String name, Label label) {
                breakLabels.push(new JSLabel(name, label));
            }

            void popBreakLabel() {
                breakLabels.pop();
            }

            void pushContinueLabel(String name, Label label) {
                continueLabels.push(new JSLabel(name, label));
            }

            void popContinueLabel() {
                continueLabels.pop();
            }

            // If you need a fresh Register, you have to call this.
            Register freshRegister() {
                numOfRegisters++;
                return new Register(numOfRegisters);
            }

            Register getParamRegister(int i) {
            	return paramRegisters[i];
            }

			Register getParamRegister(String name) {
				int index = params.indexOf(name);
				if (index == -1)
					throw new Error(name + " is not a parameter");
				return paramRegisters[index + 1];
			}

            int getNumberOfLocals() {
                return this.staticLocals.size() + this.maxNumOfDynamicLocals;
            }

            int getNumberOfGPRegisters() {
            	return numOfRegisters;
            }
        }

        LinkedList<Frame> frameList = new LinkedList<Frame>();

		Environment() {
		}

        Frame getCurrentFrame() {
        	return frameList.getFirst();
        }

        // It's necessary to call the method before
        // compiling IASTProgram and IASTFunction.
		void openFrame(List<String> params, List<String> argumentsVars, List<String> frameVars, List<String> regVars, boolean hasFrame) {
			frameList.addFirst(new Frame(params, argumentsVars, frameVars, regVars, hasFrame));
        }

        // It's necessary to call the method after
        // compiling IASTProgram and IASTFunction.
        void closeFrame() {
            getCurrentFrame().close();
            this.frameList.removeFirst();
        }

		// "getVar" method fetch arg/local's storage location from given
		// identifier.
        // For example: if Location is { isLocal:true, depth:1, n:2 },
        //              you can push a bytecode "setlocal 1 2 x" or "getlocal y 1 2".
        static class Location {
            static final int IN_FRAME = 0;
            static final int IN_ARGUMENTS = 1;
            static final int IN_REGISTER = 2;
            int locationType;
            boolean _isLocal;
            int depth;
            int idx;
			Register reg;

			boolean inFrame() {
				return locationType == IN_FRAME;
			}

			boolean inReg() {
				return locationType == IN_REGISTER;
			}

			boolean isLocal() {
				return _isLocal;
			}

			Location(Register reg) {
				this.locationType = IN_REGISTER;
				this._isLocal = true;
				this.depth = 0;
				this.reg = reg;
			}
            Location(int locationType, boolean isLocal, int depth, int idx) {
                this.locationType = locationType;
                this._isLocal = isLocal;
                this.depth = depth;
                this.idx = idx;
            }
        }

        Location getVar(String id) {
            int depth = 0;
            boolean isLocal = true;
            
            /* register */
            {
				Register r = getCurrentFrame().regHash.get(id);
				if (r != null)
					return new Location(r);
            }

            /* frame and arguemnts */
            for (Frame ve : frameList) {
                int n;

                if ((n = ve.staticLocals.indexOf(id)) >= 0)
                    return new Location(Location.IN_FRAME, isLocal, depth, n);
				if ((n = ve.argumentsVars.indexOf(id)) >= 0)
                    return new Location(Location.IN_ARGUMENTS, isLocal, depth, n);
                if (ve.hasFrame)
                    depth++;
                isLocal = false;
            }
            return null;
        }

		Location getVarInitialLocation(String id) {
			return new Location(getCurrentFrame().getParamRegister(id));
        }

        Label getBreakLabel(String name) {
            Frame f = getCurrentFrame();
            if (name == null) {
                return f.breakLabels.getFirst().label;
            } else {
                for (JSLabel jsl : f.breakLabels) {
					if (name.equals(jsl.name))
						return jsl.label;
                }
            }
            return null;
        }

        Label getContinueLabel(String name) {
            Frame f = getCurrentFrame();
            if (name == null) {
                return f.continueLabels.getFirst().label;
            } else {
                for (JSLabel jsl : f.continueLabels) {
					if (name.equals(jsl.name))
						return jsl.label;
               }
            }
            return null;
        }

        int getNumberOfLocals() {
            return this.getCurrentFrame().getNumberOfLocals();
        }

        int getNumberOfGPRegisters() {
        	return this.getCurrentFrame().getNumberOfGPRegisters();
        }
        
        void setRegOfGlobalObj(Register reg) {
            this.getCurrentFrame().registerOfGlobalObj = reg;
        }

        Register getRegOfGlobalObj() {
            return this.getCurrentFrame().registerOfGlobalObj;
        }
    }

	public CodeGenerator(Main.Info.OptLocals optLocals) {
		this.optLocals = optLocals;
        needArguments = false;
        needFrame = false;
    }
    
    BCBuilder bcBuilder;
    Environment env;
    Register reg;
	Main.Info.OptLocals optLocals;
    static final int THIS_OBJECT_REGISTER = 0;
    boolean needArguments;
    boolean needFrame;

    void printByteCode(List<BCode> bcodes) {
        for (BCode bcode : bcodes) {
            System.out.println(bcode);
        }
    }

    public BCBuilder compile(IASTProgram node) {
        this.bcBuilder = new BCBuilder();
        this.env = new Environment();
        try {
            bcBuilder.openFunctionBCBuilder();
			env.openFrame(new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), true);
            compileNode(node, null);
            bcBuilder.setNumberOfLocals(0);
            bcBuilder.setNumberOfGPRegisters(env.getNumberOfGPRegisters());
            env.closeFrame();
            bcBuilder.closeFuncBCBuilder();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return bcBuilder;
    }

    void compileNode(IASTNode node, Register reg) {
        Register tmp = this.reg;
        this.reg = reg;
        node.accept(this);
        this.reg = tmp;
    }

    @Override
    public Object visitProgram(IASTProgram node) {
        Register globalObjReg = env.getCurrentFrame().getParamRegister(THIS_OBJECT_REGISTER);
        env.setRegOfGlobalObj(globalObjReg);
        Label callEntry = new Label();
        Label sendEntry = new Label();
        bcBuilder.setEntry(callEntry, sendEntry);
        bcBuilder.push(callEntry);
        bcBuilder.push(sendEntry);
        bcBuilder.push(new IGetglobalobj(globalObjReg));
        bcBuilder.pushMsetfl();
        Register retReg = env.getCurrentFrame().freshRegister();
        compileNode(node.program.body, retReg);
        bcBuilder.push(new ISeta(retReg));
        bcBuilder.push(new IRet());
        return null;
    }

    @Override
    public Object visitStringLiteral(IASTStringLiteral node) {
        bcBuilder.push(new IString(reg, node.value));
        return null;
    }

    @Override
    public Object visitNumericLiteral(IASTNumericLiteral node) {
        if (node.isInteger()) {
            bcBuilder.push(new IFixnum(reg, (int) node.value));
        } else {
            bcBuilder.push(new INumber(reg, node.value));
        }
        return null;
    }

    @Override
    public Object visitBooleanLiteral(IASTBooleanLiteral node) {
        bcBuilder.push(new IBooleanconst(reg, node.value));
        return null;
    }

    @Override
    public Object visitNullLiteral(IASTNullLiteral node) {
        bcBuilder.push(new INullconst(reg));
        return null;
    }

    @Override
    public Object visitRegExpLiteral(IASTRegExpLiteral node) {
        bcBuilder.push(new IRegexp(reg, 0, node.pattern));
        return null;
    }

    @Override
    public Object visitBlockStatement(IASTBlockStatement node) {
        bcBuilder.push(new IUndefinedconst(reg));
        for (IASTStatement stmt : node.stmts)
            compileNode(stmt, reg);
        return null;
    }

    @Override
    public Object visitReturnStatement(IASTReturnStatement node) {
        if (node.value == null) {
            bcBuilder.push(new IUndefinedconst(reg));
        } else {
          compileNode(node.value, reg);
        }
        bcBuilder.push(new ISeta(reg));
        bcBuilder.push(new IRet());
        return null;
    }

    @Override
    public Object visitWithStatement(IASTWithStatement node) {
        throw new UnsupportedOperationException("WithStatement has not been implemented yet.");

    }

    @Override
	public Object visitEmptyStatement(IASTEmptyStatement node) {
		return null;
	}

    @Override
    public Object visitIfStatement(IASTIfStatement node) {
        Label l1 = new Label();
        compileNode(node.test, reg);
        bcBuilder.push(new IJumpfalse(reg, l1));
        compileNode(node.consequent, reg);
        if (node.alternate == null) {
            bcBuilder.push(l1);
        } else {
            Label l2 = new Label();
            bcBuilder.push(new IJump(l2));
            bcBuilder.push(l1);
            compileNode(node.alternate, reg);
            bcBuilder.push(l2);
        }
        return null;
    }

    @Override
    public Object visitSwitchStatement(IASTSwitchStatement node) {
        Register discReg = env.getCurrentFrame().freshRegister();
        compileNode(node.discriminant, discReg);
        LinkedList<Label> caseLabels = new LinkedList<Label>();
        Register testReg = env.getCurrentFrame().freshRegister();
        for (IASTSwitchStatement.CaseClause caseClause : node.cases) {
            if (caseClause.test != null) {
                Label caseLabel = new Label();
                compileNode(caseClause.test, testReg);
                bcBuilder.push(new IEq(reg, discReg, testReg));
                bcBuilder.push(new IJumptrue(reg, caseLabel));
                caseLabels.add(caseLabel);
            } else {
                Label caseLabel = new Label();
                bcBuilder.push(new IJump(caseLabel));
                caseLabels.add(caseLabel);
                break;
            }
        }
        Label breakLabel = new Label();
        env.getCurrentFrame().pushBreakLabel(node.label, breakLabel);
        for (IASTSwitchStatement.CaseClause caseClause : node.cases) {
            Label caseLabel = caseLabels.pollFirst();
            if (caseLabel != null) {
                bcBuilder.push(caseLabel);
            }
            compileNode(caseClause.consequent, testReg);
        }
        env.getCurrentFrame().popBreakLabel();
        bcBuilder.push(breakLabel);
        return null;
    }

    @Override
    public Object visitExpressionStatement(IASTExpressionStatement node) {
        compileNode(node.exp, reg);
        return null;
    }

    @Override
    public Object visitThrowStatement(IASTThrowStatement node) {
        throw new UnsupportedOperationException("ThrowStatement has not been implemented yet.");
    }

    @Override
    public Object visitTryCatchStatement(IASTTryCatchStatement node) {
        throw new UnsupportedOperationException("TryCatchStatement has not been implemented yet.");
    }

    @Override
    public Object visitTryFinallyStatement(IASTTryFinallyStatement node) {
        throw new UnsupportedOperationException("TryFinallyStatement has not been implemented yet.");
    }

    @Override
    public Object visitForStatement(IASTForStatement node) {
        if (node.init != null)
            compileNode(node.init, env.getCurrentFrame().freshRegister());
        Label l1 = new Label();
        Label l2 = new Label();
        Label continueLabel = new Label();
        Label breakLabel = new Label();
        bcBuilder.push(new IJump(l1));
        bcBuilder.push(l2);
        if (node.body != null) {
            env.getCurrentFrame().pushBreakLabel(node.label, breakLabel);
            env.getCurrentFrame().pushContinueLabel(node.label, continueLabel);
            compileNode(node.body, reg);
            env.getCurrentFrame().popBreakLabel();
            env.getCurrentFrame().popContinueLabel();
        }
        bcBuilder.push(continueLabel);
        if (node.update != null)
            compileNode(node.update, env.getCurrentFrame().freshRegister());
        bcBuilder.push(l1);
        Register testReg = env.getCurrentFrame().freshRegister();
        if (node.test != null) {
            compileNode(node.test, testReg);
            bcBuilder.push(new IJumptrue(testReg, l2));
        } else {
            bcBuilder.push(new IJump(l2));
        }
        bcBuilder.push(breakLabel);
        return null;
    }

    @Override
    public Object visitWhileStatement(IASTWhileStatement node) {
        Label l1 = new Label();
        Label l2 = new Label();
        Label breakLabel = new Label();
        Label continueLabel = new Label();
        bcBuilder.push(new IJump(l1));
        bcBuilder.push(l2);
        if (node.body != null) {
            env.getCurrentFrame().pushBreakLabel(node.label, breakLabel);
            env.getCurrentFrame().pushContinueLabel(node.label, continueLabel);
            compileNode(node.body, reg);
            env.getCurrentFrame().popBreakLabel();
            env.getCurrentFrame().popContinueLabel();
        }
        bcBuilder.push(continueLabel);
        bcBuilder.push(l1);
        Register testReg = env.getCurrentFrame().freshRegister();
        compileNode(node.test, testReg);
        bcBuilder.push(new IJumptrue(testReg, l2));
        bcBuilder.push(breakLabel);
        return null;
    }

    @Override
    public Object visitDoWhileStatement(IASTDoWhileStatement node) {
        Label l1 = new Label();
        Label breakLabel = new Label();
        Label continueLabel = new Label();
        bcBuilder.push(continueLabel);
        bcBuilder.push(l1);
        if (node.body != null) {
            env.getCurrentFrame().pushBreakLabel(node.label, breakLabel);
            env.getCurrentFrame().pushContinueLabel(node.label, continueLabel);
            compileNode(node.body, reg);
            env.getCurrentFrame().popBreakLabel();
            env.getCurrentFrame().popContinueLabel();
        }
        Register testReg = env.getCurrentFrame().freshRegister();
        compileNode(node.test, testReg);
        bcBuilder.push(new IJumptrue(testReg, l1));
        bcBuilder.push(breakLabel);
        return null;
    }

    @Override
    public Object visitForInStatement(IASTForInStatement node) {
        Register objReg = env.getCurrentFrame().freshRegister();
        Register iteReg = env.getCurrentFrame().freshRegister();
        Register propReg = env.getCurrentFrame().freshRegister();
        Register testReg = env.getCurrentFrame().freshRegister();
        Label breakLabel = new Label();
        Label continueLabel = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        compileNode(node.object, objReg);
        bcBuilder.push(new IMakeiterator(objReg, iteReg));
        bcBuilder.push(l1);
        bcBuilder.push(new INextpropname(objReg, iteReg, propReg));
        compileSetVariable(node.var, propReg);
        bcBuilder.push(new IIsundef(testReg, propReg));
        bcBuilder.push(new IJumptrue(testReg, l2));
        bcBuilder.push(continueLabel);
        env.getCurrentFrame().pushBreakLabel(node.label, breakLabel);
        env.getCurrentFrame().pushContinueLabel(node.label, continueLabel);
        compileNode(node.body, reg);
        env.getCurrentFrame().popBreakLabel();
        env.getCurrentFrame().popContinueLabel();
        bcBuilder.push(new IJump(l1));
        bcBuilder.push(breakLabel);
        bcBuilder.push(l2);
        return null;
    }

    @Override
    public Object visitBreakStatement(IASTBreakStatement node) {
        Label label = env.getBreakLabel(node.label);
        if (label != null) {
            bcBuilder.push(new IJump(label));
        } else {
            throw new Error("Compile Error ... BreakStatement");
        }
        return null;
    }

    @Override
    public Object visitContinueStatement(IASTContinueStatement node) {
        Label label = env.getContinueLabel(node.label);
        if (label != null) {
            bcBuilder.push(new IJump(label));
        } else {
            throw new Error("Compile Error ... ContinueStatement");
        }
        return null;
    }
    
    // precondition: node.params and node.locals are disjoint
    @Override
    public Object visitFunctionExpression(IASTFunctionExpression node) {
        boolean savedNeedArguments = needArguments;
        boolean savedNeedFrame = needFrame;
        needArguments = node.needArguments;
        needFrame = node.needFrame;
        bcBuilder.openFunctionBCBuilder();
        int functionIdx = bcBuilder.getFBIdx();

        LinkedList<String> locals = new LinkedList<String>();
        LinkedList<String> params = new LinkedList<String>();
        LinkedList<String> regLocals = new LinkedList<String>();

        if (needArguments) {
            params.addAll(node.params);
            locals.addFirst("arguments");
        } else {
			// PROSYM: no variables are in innerUsedLocals if needArguments == false
			// G1: all variables are in innerUsedLocals
            for (String var : node.params) {
                if (node.innerUsedLocals == null || node.innerUsedLocals.contains(var))
                    locals.add(var);
                else
                    regLocals.add(var);
            }
        }
		// PROSYM and G1: all variables are in innerUsedLocals
        for (String var : node.locals) {
                if (node.innerUsedLocals == null || node.innerUsedLocals.contains(var))
                    locals.add(var);
                else
                    regLocals.add(var);
        }

		env.openFrame(node.params, params, locals, regLocals, needFrame);

        Register globalObjReg = env.getCurrentFrame().getParamRegister(THIS_OBJECT_REGISTER);
        env.setRegOfGlobalObj(globalObjReg);
        
        bcBuilder.push(new MParameter(env.getCurrentFrame().getParamRegister(THIS_OBJECT_REGISTER)));
        for (String var: node.params)
        		bcBuilder.push(new MParameter(env.getCurrentFrame().getParamRegister(var)));
        Label callEntry = new Label();
        Label sendEntry = new Label();
        bcBuilder.setEntry(callEntry, sendEntry);
        bcBuilder.push(callEntry);
        bcBuilder.push(new IGetglobalobj(globalObjReg));
        bcBuilder.push(sendEntry);

        if (needFrame)
			bcBuilder.push(new INewframe(locals.size(), needArguments ? 1 : 0));
        bcBuilder.pushMsetfl();

		/*
		 * move argument on stack to appropriate location, i.e, frame
		 */
        if (!needArguments && needFrame) {
			for (String name : node.params) {
                Environment.Location varLoc = env.getVar(name);
				Environment.Location varInitLoc = env.getVarInitialLocation(name);
                if (varLoc.inFrame())
					bcBuilder.push(new ISetlocal(0, varLoc.idx, varInitLoc.reg));
            }
        }

        Register retReg = env.getCurrentFrame().freshRegister();
        compileNode(node.body, retReg);
        bcBuilder.push(new IUndefinedconst(reg));
        bcBuilder.push(new ISeta(reg));
        bcBuilder.push(new IRet());

        bcBuilder.setNumberOfLocals(env.getNumberOfLocals());
        bcBuilder.setNumberOfGPRegisters(env.getNumberOfGPRegisters());

        // Don't change the order.
        env.closeFrame();
        bcBuilder.closeFuncBCBuilder();

        bcBuilder.push(new IMakeclosure(reg, functionIdx));
        needArguments = savedNeedArguments;
        needFrame = savedNeedFrame;
        return null;
    }

    @Override
    public Object visitThisExpression(IASTThisExpression node) {
        bcBuilder.push(new IMove(reg, env.getRegOfGlobalObj()));
        return null;
    }

    @Override
    public Object visitArrayExpression(IASTArrayExpression node) {
        Register r1 = env.getCurrentFrame().freshRegister();
        Register constructorReg = env.getCurrentFrame().freshRegister();
        Register[] tmpRegs = new Register[1];
        tmpRegs[0] = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IString(r1, "Array"));
        bcBuilder.push(new IGetglobal(constructorReg, r1));
        bcBuilder.push(new IFixnum(tmpRegs[0], node.elements.size()));
        bcBuilder.push(new INew(reg, constructorReg));
        bcBuilder.pushMCall(reg, constructorReg, tmpRegs, true, false);
        bcBuilder.pushMsetfl();
        bcBuilder.push(new IGeta(reg));
        int i = 0;
        for (IASTExpression element : node.elements) {
            compileNode(element, r1);
            bcBuilder.push(new ISetarray(reg, i++, r1));
        }
        return null;
    }

    @Override
    public Object visitObjectExpression(IASTObjectExpression node) {
        Register r1 = env.getCurrentFrame().freshRegister();
        Register constructorReg = env.getCurrentFrame().freshRegister();
        Register[] tmpRegs = new Register[0];
        bcBuilder.push(new IString(r1, "Object"));
        bcBuilder.push(new IGetglobal(constructorReg, r1));
        bcBuilder.push(new INew(reg, constructorReg));
        bcBuilder.pushMCall(reg, constructorReg, tmpRegs, true, false);
        bcBuilder.pushMsetfl();
        bcBuilder.push(new IGeta(reg));
        for (IASTObjectExpression.Property prop : node.properties) {
            compileNode(prop.value, r1);
            compileNode(prop.key, constructorReg);
            bcBuilder.push(new ISetprop(reg, constructorReg, r1));
        }
        return null;
    }

    @Override
    public Object visitUnaryExpression(IASTUnaryExpression node) {
        switch (node.operator) {
        case PLUS: {
            compileNode(node.operands[0], reg);
		}
			break;
        case MINUS: {
            Register r1 = env.getCurrentFrame().freshRegister();
            Register r2 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], r1);
            bcBuilder.push(new IFixnum(r2, -1));
            bcBuilder.push(new IMul(reg, r1, r2));
		}
			break;
        case NOT: {
            Register r1 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], r1);
            bcBuilder.push(new INot(reg, r1));
		}
			break;
        case INC:
        case DEC: {
            if (node.prefix) {
                Register r1 = env.getCurrentFrame().freshRegister();
                Register r2 = env.getCurrentFrame().freshRegister();
                compileNode(node.operands[0], r1);
                bcBuilder.push(new IFixnum(r2, 1));
                if (node.operator == IASTUnaryExpression.Operator.INC) {
                    bcBuilder.push(new IAdd(reg, r1, r2));
                } else if (node.operator == IASTUnaryExpression.Operator.DEC) {
                    bcBuilder.push(new ISub(reg, r1, r2));
                }
                compileAssignment(node.operands[0], reg);
            } else {
                Register r1 = env.getCurrentFrame().freshRegister();
                Register r2 = env.getCurrentFrame().freshRegister();
                compileNode(node.operands[0], reg);
                bcBuilder.push(new IFixnum(r1, 1));
                if (node.operator == IASTUnaryExpression.Operator.INC) {
                    bcBuilder.push(new IAdd(r2, reg, r1));
                } else if (node.operator == IASTUnaryExpression.Operator.DEC) {
                    bcBuilder.push(new ISub(r2, reg, r1));
                }
                compileAssignment(node.operands[0], r2);
            }
		}
			break;
        case BNOT: {
            Register r1 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IFixnum(r1, 0));
            bcBuilder.push(new ISub(reg, r1, reg));
            bcBuilder.push(new IFixnum(r1, 1));
            bcBuilder.push(new ISub(reg, reg, r1));
		}
			break;
        case TYPEOF:
            throw new UnsupportedOperationException("Unary operator not implemented : typeof");
        case VOID: {
            Register r1 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IString(r1, "\"undefined\""));
            bcBuilder.push(new IGetglobal(reg, r1));
		}
			break;
        case DELETE:
            throw new UnsupportedOperationException("Unary operator not implemented : delete");
        default:
            throw new UnsupportedOperationException("Unary operator not implemented : unknown");
        }
        return null;
    }
    @Override
    public Object visitBinaryExpression(IASTBinaryExpression node) {
        Register r1 = null, r2 = null;
        switch (node.operator) {
        case ADD: case SUB: case MUL: case DIV: case MOD:
        case SHL: case SHR: case UNSIGNED_SHR:
        case BAND: case BOR: case BXOR:
        case EQUAL: case NOT_EQUAL: case EQ: case NOT_EQ: case LT: case LTE: case GT: case GTE:
        case ASSIGN_ADD: case ASSIGN_SUB: case ASSIGN_MUL: case ASSIGN_DIV: case ASSIGN_MOD:
        case ASSIGN_SHL: case ASSIGN_SHR: case ASSIGN_UNSIGNED_SHR:
        case ASSIGN_BAND: case ASSIGN_BOR: case ASSIGN_BXOR:
            r1 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], r1);
            r2 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[1], r2);
        }


        switch (node.operator) {
        // arithmetic
        case ADD: case ASSIGN_ADD: {
            bcBuilder.push(new IAdd(reg, r1, r2));
        } break;
        case SUB: case ASSIGN_SUB: {
            bcBuilder.push(new ISub(reg, r1, r2));
        } break;
        case MUL: case ASSIGN_MUL: {
            bcBuilder.push(new IMul(reg, r1, r2));
        } break;
        case DIV: case ASSIGN_DIV: {
            bcBuilder.push(new IDiv(reg, r1, r2));
        } break;
        case MOD: case ASSIGN_MOD: {
            bcBuilder.push(new IMod(reg, r1, r2));
        } break;

        // shift
        case SHL: case ASSIGN_SHL: {
            bcBuilder.push(new ILeftshift(reg, r1, r2));
        } break;
        case SHR: case ASSIGN_SHR: {
            bcBuilder.push(new IRightshift(reg, r1, r2));
        } break;
        case UNSIGNED_SHR: case ASSIGN_UNSIGNED_SHR: {
            bcBuilder.push(new IUnsignedrightshift(reg, r1, r2));
        } break;

        // bit
        case BOR: case ASSIGN_BOR: {
            bcBuilder.push(new IBitor(reg, r1, r2));
        } break;
        case BAND: case ASSIGN_BAND: {
            bcBuilder.push(new IBitand(reg, r1, r2));
        } break;
        case BXOR: case ASSIGN_BXOR: {
            Register r3 = env.getCurrentFrame().freshRegister();
            bcBuilder.push(new IBitor(r3, r1, r2));  // r3 = a | b
            bcBuilder.push(new IBitand(r1, r1, r2));  // r1 = a & b
            bcBuilder.push(new IFixnum(r2, 0));
            bcBuilder.push(new ISub(r1, r2, r1));    // r1 = -(a & b)
            bcBuilder.push(new IFixnum(r2, 1));
            bcBuilder.push(new ISub(r1, r1, r2));    // r1 = ~(a & b)
            bcBuilder.push(new IBitand(reg, r1, r3)); // reg = ~(a & b) & (a | b)
        } break;

        // logical
        case OR: {
            Label l1 = new Label();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IJumptrue(reg, l1));
            compileNode(node.operands[1], reg);
            bcBuilder.push(l1);
		}
			break;
        case AND: {
            Label l1 = new Label();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IJumpfalse(reg, l1));
            compileNode(node.operands[1], reg);
            bcBuilder.push(l1);
        }

        // relational
        case EQUAL: case NOT_EQUAL: {
            Register r3 = (node.operator == IASTBinaryExpression.Operator.EQUAL) ? reg : env.getCurrentFrame().freshRegister();
            Register r4 = env.getCurrentFrame().freshRegister();
            Register r5 = env.getCurrentFrame().freshRegister();
            Register r6 = env.getCurrentFrame().freshRegister();
            Register r7 = env.getCurrentFrame().freshRegister();
            Register r8 = env.getCurrentFrame().freshRegister();
            Register r9 = env.getCurrentFrame().freshRegister();
            // Register r10 = node.operator == IASTBinaryExpression.Operator.EQUAL ? reg : env.freshRegister();
            Label l1 = new Label();
            Label l2 = new Label();
            Label l3 = new Label();
            Label l4 = new Label();
            bcBuilder.push(new IEqual(r3, r1, r2));
            bcBuilder.push(new IIsundef(r4, r3));
            bcBuilder.push(new IJumpfalse(r4, l1));
            bcBuilder.push(new IString(r5, "valueOf"));
            bcBuilder.push(new IIsobject(r6, r1));
            bcBuilder.push(new IJumpfalse(r6, l2));

            bcBuilder.push(new IGetprop(r7, r1, r5));
            bcBuilder.push(new MCall(r1, r7, new Register[] {}, false, false));
            bcBuilder.push(new IGeta(r8));
            bcBuilder.push(new IMove(r9, r2));

            bcBuilder.push(new IJump(l3));

            bcBuilder.push(l2);
            bcBuilder.push(new IGetprop(r7, r2, r5));
            bcBuilder.push(new MCall(r2, r7, new Register[] {}, false, false));
            bcBuilder.push(new IGeta(r9));
            bcBuilder.push(new IMove(r8, r1));

            bcBuilder.push(l3);
            bcBuilder.push(new IEqual(r3, r8, r9));
            bcBuilder.push(new IIsundef(r5, r3));
            bcBuilder.push(new IJumpfalse(r5, l4));
            bcBuilder.push(new IError(r3, "\"EQUAL_GETTOPRIMITIVE\""));
            bcBuilder.push(l1);
            bcBuilder.push(l4);
            if (node.operator == IASTBinaryExpression.Operator.NOT_EQUAL)
                bcBuilder.push(new INot(reg, r3));
        } break;
        case EQ: {
            bcBuilder.push(new IEq(reg, r1, r2));
        } break;
        case NOT_EQ: {
            Register r3 = env.getCurrentFrame().freshRegister();
            bcBuilder.push(new IEq(r3, r1, r2));
            bcBuilder.push(new INot(reg, r3));
        } break;
        case LT: {
            bcBuilder.push(new ILessthan(reg, r1, r2));
        } break;
        case LTE: {
            bcBuilder.push(new ILessthanequal(reg, r1, r2));
        } break;
        case GT: {
            bcBuilder.push(new ILessthan(reg, r2, r1));
        } break;
        case GTE: {
            bcBuilder.push(new ILessthanequal(reg, r2, r1));
        } break;
        case IN:
            throw new UnsupportedOperationException("Binary operator not implemented : in");
        case INSTANCE_OF: {
            bcBuilder.push(new IInstanceof(reg, r2, r1));
        } break;

        // assignment
        case ASSIGN: {
            compileNode(node.operands[1], reg);
        } break;
        }


        switch (node.operator) {
        case ASSIGN_ADD: case ASSIGN_SUB: case ASSIGN_MUL: case ASSIGN_DIV: case ASSIGN_MOD:
        case ASSIGN_SHL: case ASSIGN_SHR: case ASSIGN_UNSIGNED_SHR:
        case ASSIGN_BAND: case ASSIGN_BOR: case ASSIGN_BXOR:
        case ASSIGN: {
            compileAssignment(node.operands[0], reg);
        }
        }
        return null;
    }
    void compileAssignment(IASTExpression dst, Register srcReg) {
        if (dst instanceof IASTIdentifier) {
            String id = ((IASTIdentifier) dst).id;
            compileSetVariable(id, srcReg);
        } else if (dst instanceof IASTMemberExpression) {
            IASTMemberExpression memExp = (IASTMemberExpression) dst;
            Register objReg = env.getCurrentFrame().freshRegister();
            compileNode(memExp.object, objReg);
            Register propReg = env.getCurrentFrame().freshRegister();
            compileNode(memExp.property, propReg);
            bcBuilder.push(new ISetprop(objReg, propReg, srcReg));
        }
    }

    void compileSetVariable(String varName, Register srcReg) {
        Environment.Location varLoc = env.getVar(varName);
        if (varLoc == null) { // global
            Register r1 = env.getCurrentFrame().freshRegister();
            bcBuilder.push(new IString(r1, varName));
            bcBuilder.push(new ISetglobal(r1, srcReg));
        } else {
            if (varLoc.inReg()) {
				bcBuilder.push(new IMove(varLoc.reg, srcReg));
            } else if (varLoc.inFrame()) {
                if (!needArguments && !needFrame && varLoc.isLocal())
                    throw new Error("internal error");
                bcBuilder.push(new ISetlocal(varLoc.depth, varLoc.idx, srcReg));
            } else { // in arguments
                if (!needArguments && varLoc.isLocal())
                    throw new Error("internal error");
                bcBuilder.push(new ISetarg(varLoc.depth, varLoc.idx, srcReg));
            }
        }
    }

    void compileGetVariable(String varName, Register dstReg) {
        Environment.Location varLoc = env.getVar(varName);
        if (varLoc == null) {
            Register r1 = env.getCurrentFrame().freshRegister();
            bcBuilder.push(new IString(r1, varName));
            bcBuilder.push(new IGetglobal(dstReg, r1));
        } else {
            if (varLoc.inReg()) {
				bcBuilder.push(new IMove(dstReg, varLoc.reg));
            } else if (varLoc.inFrame()) {
                if (!needArguments && !needFrame && varLoc.isLocal())
                    throw new Error("internal error");
                bcBuilder.push(new IGetlocal(dstReg, varLoc.depth, varLoc.idx));
            } else { // in arguments
                if (!needArguments && varLoc.isLocal())
                    throw new Error("internal error");
                bcBuilder.push(new IGetarg(dstReg, varLoc.depth, varLoc.idx));
            }
        }
    }

    @Override
    public Object visitTernaryExpression(IASTTernaryExpression node) {
        switch (node.operator) {
        case COND: {
            Register testReg = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], testReg);
            Label l1 = new Label();
            Label l2 = new Label();
            bcBuilder.push(new IJumpfalse(testReg, l1));
            compileNode(node.operands[1], reg);
            bcBuilder.push(new IJump(l2));
            bcBuilder.push(l1);
            compileNode(node.operands[2], reg);
            bcBuilder.push(l2);
		}
			break;
        default:
            throw new Error("Unreachable code.");
        }
        return null;
    }

    @Override
    public Object visitCallExpression(IASTCallExpression node) {
        Register[] tmpRegs = new Register[node.arguments.size()];
        for (int i = 0; i < tmpRegs.length; i++)
            tmpRegs[i] = env.getCurrentFrame().freshRegister();

    	if (node.callee instanceof IASTMemberExpression) {
    		IASTNode object = ((IASTMemberExpression) node.callee).object;
    		IASTNode property = ((IASTMemberExpression) node.callee).property;
    		Register objReg      = env.getCurrentFrame().freshRegister();
    		Register propNameReg = env.getCurrentFrame().freshRegister();
    		Register propValReg   = env.getCurrentFrame().freshRegister();
    		
    		compileNode(object,   objReg);
    		compileNode(property, propNameReg);
    		bcBuilder.push(new IGetprop(propValReg, objReg, propNameReg));
            for (int i = 0; i < node.arguments.size(); i++) {
            	IASTNode argument = node.arguments.get(i);
                compileNode(argument, tmpRegs[i]);
            }
    		bcBuilder.pushMCall(objReg, propValReg, tmpRegs, false, false);
    	} else {
            Register calleeReg = env.getCurrentFrame().freshRegister();
            compileNode(node.callee, calleeReg);
            for (int i = 0; i < node.arguments.size(); i++) {
            	IASTNode argument = node.arguments.get(i);
                compileNode(argument, tmpRegs[i]);
            }            
            bcBuilder.pushMCall(null, calleeReg, tmpRegs, false, false);
    	}
        bcBuilder.pushMsetfl();
        bcBuilder.push(new IGeta(reg));
        return null;
    }

    @Override
    public Object visitNewExpression(IASTNewExpression node) {
        Register[] tmpRegs = new Register[node.arguments.size()];
        for (int i = 0; i < tmpRegs.length; i++)
            tmpRegs[i] = env.getCurrentFrame().freshRegister();
    	Register constructorReg = env.getCurrentFrame().freshRegister();
        compileNode(node.constructor, constructorReg);
        for (int i = 0; i < node.arguments.size(); i++) {
        	IASTNode argument = node.arguments.get(i);
            compileNode(argument, tmpRegs[i]);
        }
        bcBuilder.push(new INew(reg, constructorReg));
        bcBuilder.pushMCall(reg, constructorReg, tmpRegs, true, false);

        bcBuilder.pushMsetfl();
        Register strReg = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IString(strReg, "Object"));
        Register objReg = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IGetglobal(objReg, strReg));
        Register resultOfNewSendReg = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IGeta(resultOfNewSendReg));
        Register result = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IInstanceof(result, resultOfNewSendReg, objReg));
        Label l1 = new Label();
        bcBuilder.push(new IJumpfalse(result, l1));
        bcBuilder.push(new IGeta(reg));
        bcBuilder.push(l1);
        return null;
    }

    @Override
    public Object visitMemberExpression(IASTMemberExpression node) {
        Register objReg = env.getCurrentFrame().freshRegister();
        compileNode(node.object, objReg);
        Register expReg = env.getCurrentFrame().freshRegister();
        compileNode(node.property, expReg);
        bcBuilder.push(new IGetprop(reg, objReg, expReg));
        return null;
    }

    @Override
    public Object visitIdentifier(IASTIdentifier node) {
        compileGetVariable(node.id, reg);
        return null;
    }
}
