package com.myapps.timewrap.Wrapvideo.filters;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.FieldPacker;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.ScriptC;
import android.renderscript.Type;

public class ScriptC_ImageRotator extends ScriptC {
    private static final String __rs_resource_name = "imagerotator";
    private Element __ALLOCATION;
    private Element __I32;
    private Element __U8_4;
    private FieldPacker __rs_fp_ALLOCATION;
    private FieldPacker __rs_fp_I32;
    private int mExportVar_inHeight;
    private Allocation mExportVar_inImage;
    private int mExportVar_inWidth;

    public ScriptC_ImageRotator(RenderScript renderScript) {
        super(renderScript, __rs_resource_name, ImageRotatorBitCode.getBitCode32(), ImageRotatorBitCode.getBitCode64());
        this.__ALLOCATION = Element.ALLOCATION(renderScript);
        this.__I32 = Element.I32(renderScript);
        this.__U8_4 = Element.U8_4(renderScript);
    }

    public Allocation get_inImage() {
        return this.mExportVar_inImage;
    }

    public synchronized void set_inImage(Allocation allocation) {
        setVar(0, allocation);
        this.mExportVar_inImage = allocation;
    }

    public FieldID getFieldID_inImage() {
        return createFieldID(0, (Element) null);
    }

    public int get_inWidth() {
        return this.mExportVar_inWidth;
    }

    public synchronized void set_inWidth(int i) {
        setVar(1, i);
        this.mExportVar_inWidth = i;
    }

    public FieldID getFieldID_inWidth() {
        return createFieldID(1, (Element) null);
    }

    public int get_inHeight() {
        return this.mExportVar_inHeight;
    }

    public synchronized void set_inHeight(int i) {
        setVar(2, i);
        this.mExportVar_inHeight = i;
    }

    public FieldID getFieldID_inHeight() {
        return createFieldID(2, (Element) null);
    }

    public KernelID getKernelID_rotate_90_clockwise() {
        Element element = null;
        return createKernelID(1, 59, element, element);
    }

    public void forEach_rotate_90_clockwise(Allocation allocation, Allocation allocation2) {
        forEach_rotate_90_clockwise(allocation, allocation2, (LaunchOptions) null);
    }

    public void forEach_rotate_90_clockwise(Allocation allocation, Allocation allocation2, LaunchOptions launchOptions) {
        if (!allocation.getType().getElement().isCompatible(this.__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        } else if (allocation2.getType().getElement().isCompatible(this.__U8_4)) {
            Type type = allocation.getType();
            Type type2 = allocation2.getType();
            if (type.getCount() == type2.getCount() && type.getX() == type2.getX() && type.getY() == type2.getY() && type.getZ() == type2.getZ() && type.hasFaces() == type2.hasFaces() && type.hasMipmaps() == type2.hasMipmaps()) {
                forEach(1, allocation, allocation2, (FieldPacker) null, launchOptions);
                return;
            }
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        } else {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
    }

    public KernelID getKernelID_rotate_180_clockwise() {
        Element element = null;
        return createKernelID(2, 59, element, element);
    }

    public void forEach_rotate_180_clockwise(Allocation allocation, Allocation allocation2) {
        forEach_rotate_180_clockwise(allocation, allocation2, (LaunchOptions) null);
    }

    public void forEach_rotate_180_clockwise(Allocation allocation, Allocation allocation2, LaunchOptions launchOptions) {
        if (!allocation.getType().getElement().isCompatible(this.__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        } else if (allocation2.getType().getElement().isCompatible(this.__U8_4)) {
            Type type = allocation.getType();
            Type type2 = allocation2.getType();
            if (type.getCount() == type2.getCount() && type.getX() == type2.getX() && type.getY() == type2.getY() && type.getZ() == type2.getZ() && type.hasFaces() == type2.hasFaces() && type.hasMipmaps() == type2.hasMipmaps()) {
                forEach(2, allocation, allocation2, (FieldPacker) null, launchOptions);
                return;
            }
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        } else {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
    }

    public KernelID getKernelID_rotate_270_clockwise() {
        Element element = null;
        return createKernelID(3, 59, element, element);
    }

    public void forEach_rotate_270_clockwise(Allocation allocation, Allocation allocation2) {
        forEach_rotate_270_clockwise(allocation, allocation2, (LaunchOptions) null);
    }

    public void forEach_rotate_270_clockwise(Allocation allocation, Allocation allocation2, LaunchOptions launchOptions) {
        if (!allocation.getType().getElement().isCompatible(this.__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        } else if (allocation2.getType().getElement().isCompatible(this.__U8_4)) {
            Type type = allocation.getType();
            Type type2 = allocation2.getType();
            if (type.getCount() == type2.getCount() && type.getX() == type2.getX() && type.getY() == type2.getY() && type.getZ() == type2.getZ() && type.hasFaces() == type2.hasFaces() && type.hasMipmaps() == type2.hasMipmaps()) {
                forEach(3, allocation, allocation2, (FieldPacker) null, launchOptions);
                return;
            }
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        } else {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
    }

    public KernelID getKernelID_flip_horizontally() {
        Element element = null;
        return createKernelID(4, 59, element, element);
    }

    public void forEach_flip_horizontally(Allocation allocation, Allocation allocation2) {
        forEach_flip_horizontally(allocation, allocation2, (LaunchOptions) null);
    }

    public void forEach_flip_horizontally(Allocation allocation, Allocation allocation2, LaunchOptions launchOptions) {
        if (!allocation.getType().getElement().isCompatible(this.__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        } else if (allocation2.getType().getElement().isCompatible(this.__U8_4)) {
            Type type = allocation.getType();
            Type type2 = allocation2.getType();
            if (type.getCount() == type2.getCount() && type.getX() == type2.getX() && type.getY() == type2.getY() && type.getZ() == type2.getZ() && type.hasFaces() == type2.hasFaces() && type.hasMipmaps() == type2.hasMipmaps()) {
                forEach(4, allocation, allocation2, (FieldPacker) null, launchOptions);
                return;
            }
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        } else {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
    }

    public KernelID getKernelID_flip_vertically() {
        Element element = null;
        return createKernelID(5, 59, element, element);
    }

    public void forEach_flip_vertically(Allocation allocation, Allocation allocation2) {
        forEach_flip_vertically(allocation, allocation2, (LaunchOptions) null);
    }

    public void forEach_flip_vertically(Allocation allocation, Allocation allocation2, LaunchOptions launchOptions) {
        if (!allocation.getType().getElement().isCompatible(this.__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        } else if (allocation2.getType().getElement().isCompatible(this.__U8_4)) {
            Type type = allocation.getType();
            Type type2 = allocation2.getType();
            if (type.getCount() == type2.getCount() && type.getX() == type2.getX() && type.getY() == type2.getY() && type.getZ() == type2.getZ() && type.hasFaces() == type2.hasFaces() && type.hasMipmaps() == type2.hasMipmaps()) {
                forEach(5, allocation, allocation2, (FieldPacker) null, launchOptions);
                return;
            }
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        } else {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
    }
}
