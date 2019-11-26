package com.xj.router.compiler;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.xj.router.annotation.Module;
import com.xj.router.annotation.Modules;
import com.xj.router.annotation.Route;
import com.xj.router.compiler.target.TargetInfo;
import com.xj.router.compiler.util.UtilManager;
import com.xj.router.compiler.util.Utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

// @AutoService(Processor.class) // 生成META-INF等信息
// @SupportedSourceVersion(SourceVersion.RELEASE_7)
// @SupportedAnnotationTypes("com.ai.router.anno.Route")
public class RouterProcessor extends AbstractProcessor {
    private static final boolean DEBUG = true;
    private Messager messager;
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
        UtilManager.getMgr().init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 注解为 null，直接返回
        if (annotations == null || annotations.size() == 0) {
            return false;
        }

        UtilManager.getMgr().getMessager().printMessage(Diagnostic.Kind.NOTE, "process");
        boolean hasModule = false;
        boolean hasModules = false;
        // module
        String moduleName = "RouterMapping";
        Set<? extends Element> moduleList = roundEnv.getElementsAnnotatedWith(Module.class);
        if (moduleList != null && moduleList.size() > 0) {
            Module annotation = moduleList.iterator().next().getAnnotation(Module.class);
            moduleName = moduleName + "_" + annotation.value();
            hasModule = true;
        }
        // modules
        String[] moduleNames = null;
        Set<? extends Element> modulesList = roundEnv.getElementsAnnotatedWith(Modules.class);
        if (modulesList != null && modulesList.size() > 0) {
            Element modules = modulesList.iterator().next();
            moduleNames = modules.getAnnotation(Modules.class).value();
            hasModules = true;
        }

        debug("generate modules RouterInit annotations=" + annotations + " roundEnv=" + roundEnv);
        debug("generate modules RouterInit hasModules=" + hasModules + " hasModule=" + hasModule);
        // RouterInit
        if (hasModules) { // 有使用 @Modules 注解，生成 RouterInit 文件，适用于多个 moudle
            debug("generate modules RouterInit");
            generateModulesRouterInit(moduleNames);
        } else if (!hasModule) { // 没有使用 @Modules 注解，并且没有使用 @Module，生成相应的 RouterInit 文件，使用与单个 moudle
            debug("generate default RouterInit");
            generateDefaultRouterInit();
        }

        // 扫描 Route 自己注解
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Route.class);
        List<TargetInfo> targetInfos = new ArrayList<>();
        for (Element element : elements) {
            System.out.println("elements =" + elements);
            // 检查类型
            if (!Utils.checkTypeValid(element)) continue;
            TypeElement typeElement = (TypeElement) element;
            Route route = typeElement.getAnnotation(Route.class);
            targetInfos.add(new TargetInfo(typeElement, route.path()));
        }

        // 根据 module 名字生成相应的 java 文件
        if (!targetInfos.isEmpty()) {
            generateCode(targetInfos, moduleName);
        }
        return false;
    }

    /**
     * 生成对应的java文件
     *
     * @param targetInfos 代表router和activity
     * @param moduleName
     */
    private void generateCode(List<TargetInfo> targetInfos, String moduleName) {
        // Map<String, Class<? extends Activity>> routers

        TypeElement activityType = UtilManager
                .getMgr()
                .getElementUtils()
                .getTypeElement("android.app.Activity");

        ParameterizedTypeName actParam = ParameterizedTypeName.get(ClassName.get(Class.class),
                WildcardTypeName.subtypeOf(ClassName.get(activityType)));

        ParameterizedTypeName parma = ParameterizedTypeName.get(ClassName.get(Map.class),
                ClassName.get(String.class), actParam);

        ParameterSpec parameterSpec = ParameterSpec.builder(parma, "routers").build();

        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("map")
//                .addAnnotation(Override.class)
                .addModifiers(Modifier.STATIC)
                .addModifiers(Modifier.PUBLIC);

//                .addParameter(parameterSpec);
        for (TargetInfo info : targetInfos) {
            methodSpecBuilder.addStatement("com.xj.router.api.Router.getInstance().add($S, $T.class)", info.getRoute(), info.getTypeElement());
        }

        TypeElement interfaceType = UtilManager
                .getMgr()
                .getElementUtils()
                .getTypeElement(Constants.ROUTE_INTERFACE_NAME);

        TypeSpec typeSpec = TypeSpec.classBuilder(moduleName)
//                .addSuperinterface(ClassName.get(interfaceType))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(methodSpecBuilder.build())
                .addJavadoc("Generated by Router. Do not edit it!\n")
                .build();
        try {
            JavaFile.builder(Constants.ROUTE_CLASS_PACKAGE, typeSpec)
                    .build()
                    .writeTo(UtilManager.getMgr().getFiler());
            System.out.println("generateCode: =" + Constants.ROUTE_CLASS_PACKAGE + "." + Constants.ROUTE_CLASS_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("generateCode:e  =" + e);
        }

    }

    /**
     * 定义你的注解处理器注册到哪些注解上
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Route.class.getCanonicalName());
        annotations.add(Module.class.getCanonicalName());
        annotations.add(Modules.class.getCanonicalName());
        return annotations;
    }

    /**
     * java版本
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void generateDefaultRouterInit() {
        MethodSpec.Builder initMethod = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
        initMethod.addStatement("RouterMapping.map()");
        TypeSpec routerInit = TypeSpec.classBuilder("RouterInit")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(initMethod.build())
                .build();
        try {
            JavaFile.builder(Constants.ROUTE_CLASS_PACKAGE, routerInit)
                    .build()
                    .writeTo(mFiler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateModulesRouterInit(String[] moduleNames) {
        MethodSpec.Builder initMethod = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
        for (String module : moduleNames) {
            initMethod.addStatement("RouterMapping_" + module + ".map()");
        }
        TypeSpec routerInit = TypeSpec.classBuilder("RouterInit")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(initMethod.build())
                .build();
        try {
            JavaFile.builder(Constants.ROUTE_CLASS_PACKAGE, routerInit)
                    .build()
                    .writeTo(mFiler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void debug(String msg) {
        if (DEBUG) {
            messager.printMessage(Diagnostic.Kind.NOTE, msg);
        }
    }

}
