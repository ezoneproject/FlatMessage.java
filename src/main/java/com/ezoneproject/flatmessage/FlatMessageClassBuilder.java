package com.ezoneproject.flatmessage;

import com.ezoneproject.flatmessage.annotation.FieldDataType;
import com.ezoneproject.flatmessage.debug.FlatStringUtil;
import com.ezoneproject.flatmessage.internal.AnnotationFields;
import com.ezoneproject.flatmessage.internal.FlatFieldInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 어노테이션 빌더 클래스
 */
public class FlatMessageClassBuilder {

    private final AnnotationFields annotationFields = AnnotationFields.getInstance();

    public FlatMessageClassBuilder() {
    }

    /**
     * @param clazz 등록할 클래스
     * @return 빌더 클래스
     */
    public FlatMessageClassBuilder register(Class<?> clazz) {
        List<String> stack = new ArrayList<>();
        stack.add(clazz.getCanonicalName());

        register(clazz, stack);

        return this;
    }

    /**
     * @param clazz 클래스
     * @param stack 무한루프 체크를 위한 call stack
     */
    private void register(Class<?> clazz, List<String> stack) {
        List<FlatFieldInfo> fields = annotationFields.getFlatFieldInfoList(clazz);

        for (FlatFieldInfo field : fields) {
            if (field.dataType == FieldDataType.CLASS) {
                String clsName = field.dataClass.getCanonicalName();

                // 자기 자신 호출하는 경우 무한루프 발생하므로 차단
                if (clsName.equals(clazz.getCanonicalName()))
                    throw new AnnotationDefineException("Self reference detected: " +
                            FlatStringUtil.shortClassName(clsName) + "." + field.field.getName());

                // 지금 호출되는 클래스가 클래스 트리에 있으면 크로스 호출이 되므로 차단
                if (stack.contains(clsName))
                    throw new AnnotationDefineException("Cross reference detected: " +
                            FlatStringUtil.shortClassName(clsName) + "." + field.field.getName());

                stack.add(clsName);
                register(field.dataClass, stack);
                stack.remove(stack.size() - 1);
            }
        }

    }

}
