package com.tencent.tinker.build.auxiliaryinject;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Created by tomystang on 2016/10/10.
 */

public final class AuxiliaryInjectAdapter extends ClassVisitor {
    private final String auxiliaryClassDesc;
    private boolean isClInitExists;
    private boolean isInitExists;
    private boolean isTargetClass;
    private boolean isInjected;

    public AuxiliaryInjectAdapter(String auxiliaryClassName, ClassWriter cw) {
        super(Opcodes.ASM5, cw);
        this.auxiliaryClassDesc = fastClassNameToDesc(auxiliaryClassName);
    }

    private String fastClassNameToDesc(String className) {
        if (className.startsWith("L") && className.endsWith(";")) {
            return className;
        }
        if ("boolean".equals(className)) {
            return "Z";
        } else
        if ("byte".equals(className)) {
            return "B";
        } else
        if ("char".equals(className)) {
            return "C";
        } else
        if ("short".equals(className)) {
            return "S";
        } else
        if ("int".equals(className)) {
            return "I";
        } else
        if ("long".equals(className)) {
            return "J";
        } else
        if ("float".equals(className)) {
            return "F";
        } else
        if ("double".equals(className)) {
            return "D";
        } else {
            className = className.replace('.', '/');
            return "L" + className + ";";
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.isClInitExists = false;
        this.isInitExists = false;
        this.isTargetClass = ((access & Opcodes.ACC_INTERFACE) == 0);
        this.isInjected = false;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv != null && this.isTargetClass && !this.isInjected) {
            if ("<clinit>".equals(name)) {
                this.isClInitExists = true;
                this.isInjected = true;
                mv = new InjectImplMethodVisitor(mv);
            } else
            if ("<init>".equals(name)){
                this.isInitExists = true;
                this.isInjected = true;
                mv = new InjectImplMethodVisitor(mv);
            }
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        // If method <clinit> and <init> are not found, we should generate a <clinit>.
        if (!this.isClInitExists && !this.isInitExists) {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitLdcInsn(Type.getType(AuxiliaryInjectAdapter.this.auxiliaryClassDesc));
            mv.visitVarInsn(Opcodes.ASTORE, 0);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        super.visitEnd();
    }

    private class InjectImplMethodVisitor extends MethodVisitor {
        public InjectImplMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                super.visitLdcInsn(Type.getType(AuxiliaryInjectAdapter.this.auxiliaryClassDesc));
                super.visitVarInsn(Opcodes.ASTORE, 0);
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            if (maxStack < 1) {
                maxStack = 1;
            }
            if (maxLocals < 1) {
                maxLocals = 1;
            }
            super.visitMaxs(maxStack, maxLocals);
        }
    }
}
