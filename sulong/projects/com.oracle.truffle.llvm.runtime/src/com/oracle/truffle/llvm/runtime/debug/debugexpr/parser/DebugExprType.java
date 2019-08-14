package com.oracle.truffle.llvm.runtime.debug.debugexpr.parser;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map.Entry;

import com.oracle.truffle.llvm.runtime.debug.type.LLVMSourceArrayLikeType;
import com.oracle.truffle.llvm.runtime.debug.type.LLVMSourceBasicType;
import com.oracle.truffle.llvm.runtime.debug.type.LLVMSourceDecoratorType;
import com.oracle.truffle.llvm.runtime.debug.type.LLVMSourcePointerType;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VoidType;

public class DebugExprType {

    private final Kind kind;
    private DebugExprType innerType; // used for arrays, pointers, ...
    private static EnumMap<Kind, DebugExprType> map = new EnumMap<>(Kind.class);
    private static HashMap<String, DebugExprType> structMap = new HashMap<>();
    private int nElems;

    private DebugExprType(Kind kind, DebugExprType innerType) {
        this(kind, innerType, -1);
    }

    private DebugExprType(Kind kind, DebugExprType innerType, int nElems) {
        this.kind = kind;
        this.innerType = innerType;
        this.nElems = nElems;
    }

    public DebugExprType getInnerType() {
        return innerType;
    }

    public boolean isPointer() {
        return kind == Kind.POINTER;
    }

    public boolean isUnsigned() {
        switch (kind) {
            case UNSIGNED_CHAR:
            case UNSIGNED_INT:
            case UNSIGNED_LONG:
            case UNSIGNED_SHORT:
                return true;
            default:
                return false;
        }
    }

    public DebugExprType createPointer() {
        return new DebugExprType(Kind.POINTER, this);
    }

    public DebugExprType createArrayType(int length) {
        return new DebugExprType(Kind.ARRAY, this, length);
    }

    public boolean isIntegerType() {
        switch (kind) {
            case UNSIGNED_CHAR:
            case UNSIGNED_INT:
            case UNSIGNED_LONG:
            case UNSIGNED_SHORT:
            case SIGNED_CHAR:
            case SIGNED_INT:
            case SIGNED_LONG:
            case SIGNED_SHORT:
            case BOOL:
                return true;
            default:
                return false;
        }
    }

    public boolean isFloatingType() {
        switch (kind) {
            case FLOAT:
            case DOUBLE:
            case LONG_DOUBLE:
                return true;
            default:
                return false;
        }
    }

    public Type getLLVMRuntimeType() {
        switch (kind) {
            case VOID:
                return VoidType.INSTANCE;
            case BOOL:
                return PrimitiveType.I1;
            case UNSIGNED_CHAR:
            case SIGNED_CHAR:
                return PrimitiveType.I8;
            case UNSIGNED_SHORT:
            case SIGNED_SHORT:
                return PrimitiveType.I16;
            case UNSIGNED_INT:
            case SIGNED_INT:
                return PrimitiveType.I32;
            case UNSIGNED_LONG:
            case SIGNED_LONG:
                return PrimitiveType.I64;
            case FLOAT:
                return PrimitiveType.FLOAT;
            case DOUBLE:
                return PrimitiveType.DOUBLE;
            case LONG_DOUBLE:
                return PrimitiveType.X86_FP80;
            case POINTER:
                return new PointerType(innerType.getLLVMRuntimeType());
            case ARRAY:
                if (nElems >= 0) {
                    return new ArrayType(innerType.getLLVMRuntimeType(), nElems);
                } else {
                    return new PointerType(innerType.getLLVMRuntimeType());
                }
            default:
                return VoidType.INSTANCE;
        }
    }

    /**
     * returns the more general type in terms of implicit conversions (i.e. commonType(Int32,
     * Float32) returns Float32, commonType(Int64, UInt32) returns UInt64).
     */
    public static DebugExprType commonType(DebugExprType t1, DebugExprType t2) {
        if (t1 == t2)
            return t1;
        else if (t1 == getVoidType() || t2 == getVoidType())
            return getVoidType();
        else if (t1.isFloatingType() && t2.isFloatingType()) {
            return getFloatType(Math.max(t1.getBitSize(), t2.getBitSize()));
        } else if (t1.isIntegerType() && t2.isIntegerType()) {
            if (t1.isUnsigned() != t2.isUnsigned()) {
                // return unsigned type
                return getIntType(Math.max(t1.getBitSize(), t2.getBitSize()), false);
            } else {
                return getIntType(Math.max(t1.getBitSize(), t2.getBitSize()), !t1.isUnsigned());
            }
        } else if (t1.isIntegerType() && t2.isFloatingType()) {
            return t2;
        } else if (t1.isFloatingType() && t2.isIntegerType()) {
            return t1;
        }
        return getVoidType();
    }

    public boolean canBeCastTo(DebugExprType other) {
        switch (kind) {
            case VOID:
                return false;
            case BOOL:
                return other.isIntegerType();
            case UNSIGNED_CHAR:
            case SIGNED_CHAR:
            case UNSIGNED_SHORT:
            case SIGNED_SHORT:
            case UNSIGNED_INT:
            case SIGNED_INT:
            case UNSIGNED_LONG:
            case SIGNED_LONG:
                return other.isIntegerType() || other.isFloatingType();
            case FLOAT:
            case DOUBLE:
            case LONG_DOUBLE:
                return other.isIntegerType() || other.isFloatingType();
            case POINTER:
                return other.isPointer();
            case ARRAY:
                return other.kind == Kind.ARRAY && innerType.canBeCastTo(other.innerType) && nElems == other.nElems;
            case STRUCT:
                return false;
            case FUNCTION:
                return false;
            default:
                return false;
        }
    }

    public int getBitSize() {
        switch (kind) {
            case VOID:
                return 0;
            case BOOL:
                return 1;
            case UNSIGNED_CHAR:
            case SIGNED_CHAR:
                return 8;
            case UNSIGNED_SHORT:
            case SIGNED_SHORT:
                return 16;
            case UNSIGNED_INT:
            case SIGNED_INT:
                return 32;
            case UNSIGNED_LONG:
            case SIGNED_LONG:
                return 64;
            case FLOAT:
                return 32;
            case DOUBLE:
                return 64;
            case LONG_DOUBLE:
                return 128;
            default:
                return 0;
        }
    }

    public static DebugExprType getIntType(long sizeInBits, boolean signed) {
        Kind kind = Kind.VOID;
        if (sizeInBits > 0) {
            if (sizeInBits == 1) {
                kind = Kind.BOOL;
            } else if (sizeInBits <= 8) {
                kind = signed ? Kind.SIGNED_CHAR : Kind.UNSIGNED_CHAR;
            } else if (sizeInBits <= 16) {
                kind = signed ? Kind.SIGNED_SHORT : Kind.UNSIGNED_SHORT;
            } else if (sizeInBits <= 32) {
                kind = signed ? Kind.SIGNED_INT : Kind.UNSIGNED_INT;
            } else if (sizeInBits <= 64) {
                kind = signed ? Kind.SIGNED_LONG : Kind.UNSIGNED_LONG;
            }
        }
        if (map.containsKey(kind))
            return map.get(kind);

        DebugExprType t = new DebugExprType(kind, null);
        map.put(kind, t);
        return t;
    }

    public static DebugExprType getVoidType() {
        if (map.containsKey(Kind.VOID))
            return map.get(Kind.VOID);
        DebugExprType t = new DebugExprType(Kind.VOID, null);
        map.put(Kind.VOID, t);
        return t;
    }

    public static DebugExprType getStructType(String ident) {
        if (structMap.containsKey(ident)) {
            return structMap.get(ident);
        }
        DebugExprType t = new DebugExprType(Kind.STRUCT, null);
        structMap.put(ident, t);
        return t;
    }

    public static DebugExprType getBoolType() {
        if (map.containsKey(Kind.BOOL))
            return map.get(Kind.BOOL);
        DebugExprType t = new DebugExprType(Kind.BOOL, null);
        map.put(Kind.BOOL, t);
        return t;
    }

    public static DebugExprType getFloatType(long sizeInBits) {
        Kind kind = Kind.VOID;
        if (sizeInBits <= 32) {
            kind = Kind.FLOAT;
        } else if (sizeInBits <= 64) {
            kind = Kind.DOUBLE;
        } else if (sizeInBits <= 128) {
            kind = Kind.LONG_DOUBLE;
        }
        if (map.containsKey(kind))
            return map.get(kind);
        DebugExprType t = new DebugExprType(kind, null);
        map.put(kind, t);
        return t;
    }

    public static DebugExprType getTypeFromLLVMType(Type llvmType) {
        if (llvmType instanceof PrimitiveType) {
            return getTypeFromPrimitiveType((PrimitiveType) llvmType);
        } else if (llvmType instanceof PointerType) {
            PointerType pointerType = (PointerType) llvmType;
            return new DebugExprType(Kind.POINTER, getTypeFromLLVMType(pointerType.getPointeeType()));
        } else if (llvmType instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) llvmType;
            return new DebugExprType(Kind.ARRAY, getTypeFromLLVMType(arrayType.getElementType()));
        } else if (llvmType instanceof StructureType) {
            StructureType structureType = (StructureType) llvmType;
            return new DebugExprType(Kind.STRUCT, null);
        } else {
            return DebugExprType.getVoidType();
        }
    }

    private static DebugExprType getTypeFromPrimitiveType(PrimitiveType type) {
        switch (type.getPrimitiveKind()) {
            case I1:
                return getBoolType();
            case I8:
                return getIntType(8, true);
            case I16:
                return getIntType(16, true);
            case I32:
                return getIntType(32, true);
            case I64:
                return getIntType(64, true);
            case HALF:
            case FLOAT:
                return getFloatType(32);
            case DOUBLE:
                return getFloatType(64);
            case F128:
            case X86_FP80:
            case PPC_FP128:
                return getFloatType(128);
            default:
                return getVoidType();
        }
    }

    public static DebugExprType getTypeFromSymbolTableMetaObject(Object metaObj) {
        if (metaObj instanceof LLVMSourceBasicType) {
            LLVMSourceBasicType basicType = (LLVMSourceBasicType) metaObj;
            LLVMSourceBasicType.Kind typeKind = basicType.getKind();
            long typeSize = basicType.getSize();
            boolean signed = false;
            switch (typeKind) {
                case BOOLEAN:
                    return DebugExprType.getIntType(1, false);
                case SIGNED:
                    signed = true;
                case UNSIGNED:
                    return DebugExprType.getIntType(typeSize, signed);
                case SIGNED_CHAR:
                    signed = true;
                case UNSIGNED_CHAR:
                    return DebugExprType.getIntType(8, signed);
                case FLOATING:
                    return DebugExprType.getFloatType(typeSize);
                default:
                    return DebugExprType.getVoidType();
            }
        } else if (metaObj instanceof LLVMSourceArrayLikeType) {
            LLVMSourceArrayLikeType arrayType = (LLVMSourceArrayLikeType) metaObj;
            DebugExprType innerType = getTypeFromSymbolTableMetaObject(arrayType.getElementType(0));
            return new DebugExprType(Kind.ARRAY, innerType, arrayType.getElementCount());
        } else if (metaObj instanceof LLVMSourceDecoratorType) {
            LLVMSourceDecoratorType structType = (LLVMSourceDecoratorType) metaObj;
            return new DebugExprType(Kind.STRUCT, null);
        } else if (metaObj instanceof LLVMSourcePointerType) {
            LLVMSourcePointerType pointerType = (LLVMSourcePointerType) metaObj;
            DebugExprType baseType = getTypeFromSymbolTableMetaObject(pointerType.getBaseType());
            return new DebugExprType(Kind.POINTER, baseType);
        } else {
            return DebugExprType.getVoidType();
        }
    }

    public Object parse(Object member) {
        switch (kind) {
            case BOOL:
                return Boolean.parseBoolean(member.toString());
            case UNSIGNED_CHAR:
            case SIGNED_CHAR:
                return Character.toString((char) (Short.parseShort(member.toString())));
            case UNSIGNED_SHORT:
            case SIGNED_SHORT:
                return Short.parseShort(member.toString());
            case UNSIGNED_INT:
            case SIGNED_INT:
                return Integer.parseInt(member.toString());
            case UNSIGNED_LONG:
            case SIGNED_LONG:
                return Long.parseLong(member.toString());
            case FLOAT:
                return Float.parseFloat(member.toString());
            case DOUBLE:
            case LONG_DOUBLE:
                return Double.parseDouble(member.toString());
            case STRUCT:
            case ARRAY:
            case POINTER:
                return member;
            default:
                return null;
        }
    }

    public enum Kind {
        VOID,
        BOOL,
        UNSIGNED_CHAR,
        SIGNED_CHAR,
        UNSIGNED_SHORT,
        SIGNED_SHORT,
        UNSIGNED_INT,
        SIGNED_INT,
        UNSIGNED_LONG,
        SIGNED_LONG,
        FLOAT,
        DOUBLE,
        LONG_DOUBLE,
        POINTER,
        ARRAY,
        STRUCT,
        FUNCTION;

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof DebugExprType) {
            return equalsType((DebugExprType) obj);
        }
        return false;
    }

    public boolean equalsType(DebugExprType o) {
        if (o.kind != kind) {
            return false;
        }
        switch (kind) {
            case VOID:
            case BOOL:
            case UNSIGNED_CHAR:
            case SIGNED_CHAR:
            case UNSIGNED_SHORT:
            case SIGNED_SHORT:
            case UNSIGNED_INT:
            case SIGNED_INT:
            case UNSIGNED_LONG:
            case SIGNED_LONG:
            case FLOAT:
            case DOUBLE:
            case LONG_DOUBLE:
                return true;
            case POINTER:
            case ARRAY:
                return innerType.equals(o.innerType) && nElems == o.nElems;
            case STRUCT:
            case FUNCTION:
            default:
                return super.equals(o);
        }
    }

    @Override
    public int hashCode() {
        if (innerType != null) {
            return kind.hashCode() ^ innerType.hashCode();
        }
        return kind.hashCode();
    }

    @Override
    public String toString() {
        switch (kind) {
            case VOID:
            case BOOL:
            case UNSIGNED_CHAR:
            case SIGNED_CHAR:
            case UNSIGNED_SHORT:
            case SIGNED_SHORT:
            case UNSIGNED_INT:
            case SIGNED_INT:
            case UNSIGNED_LONG:
            case SIGNED_LONG:
            case FLOAT:
            case DOUBLE:
            case LONG_DOUBLE:
                return kind.name().toLowerCase();
            case POINTER:
                return innerType.toString() + "*";
            case ARRAY:
                return innerType.toString() + "[" + (nElems >= 0 ? nElems : "") + "]";
            case STRUCT:
                for (Entry<String, DebugExprType> e : structMap.entrySet()) {
                    if (e.getValue() == this) {
                        return kind.name() + " " + e.getKey();
                    }
                }
                return kind.name().toLowerCase();
            case FUNCTION:
                return kind.name().toLowerCase();
            default:
                return super.toString();
        }
    }

}
