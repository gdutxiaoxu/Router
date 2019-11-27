Android 开发中，组件化，模块化是一个老生常谈的问题。随着项目复杂性的增长，模块化是一个必然的趋势。除非你能忍受改一下代码，就需要六七分钟的漫长时间。

模块化，组件化随之带来的另外一个问题是页面的跳转问题，由于代码的隔离，代码之间有时候会无法互相访问。于是，路由（Router）框架诞生了。

目前用得比较多的有阿里的 ARouter，美团的 WMRouter，ActivityRouter 等。

今天，就让我们一起来看一下怎样实现一个路由框架。
实现的功能有。
1. 基于编译时注解，使用方便
2. 结果回调，每次跳转 Activity 都会回调跳转结果
3. 除了可以使用注解自定义路由，还支持手动分配路由
4. 支持多模块使用，支持组件化使用


## 使用说明


### 基本使用

第一步，在要跳转的 activity 上面注明 path，

```
@Route(path = "activity/main")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
```

在要跳转的地方


```
Router.getInstance().build("activity/main").navigation(this);
```


### 如果想在多 moule 中使用

第一步，使用 `@Modules({"app", "sdk"})` 注明总共有多少个 moudle，并分别在 moudle 中注明当前 moudle 的 名字，使用 `@Module("")` 注解。注意 @Modules({"app", "sdk"}) 要与 @Module("") 一一对应。

在主 moudle 中，

```
@Modules({"app", "moudle1"})
@Module("app")
public class RouterApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Router.getInstance().init();
    }
}
```

在 moudle1 中，

```
@Route(path = "my/activity/main")
@Module("moudle1")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
    }
}
```

这样就可以支持多模块使用了。

### 自定义注入 router

```
Router.getInstance().add("activity/three", ThreeActivity.class);
```

跳转的时候调用


```
Router.getInstance().build("activity/three").navigation(this);
```


### 结果回调

路由跳转结果回调。

```
Router.getInstance().build("my/activity/main", new RouterCallback() {
    @Override
    public boolean beforeOpen(Context context, Uri uri) { 
    // 在打开路由之前
        Log.i(TAG, "beforeOpen: uri=" + uri);
        return false;
    }

   // 在打开路由之后（即打开路由成功之后会回调）
    @Override
    public void afterOpen(Context context, Uri uri) {
        Log.i(TAG, "afterOpen: uri=" + uri);

    }

    // 没有找到改 uri
    @Override
    public void notFind(Context context, Uri uri) {
        Log.i(TAG, "notFind: uri=" + uri);

    }

    // 发生错误
    @Override
    public void error(Context context, Uri uri, Throwable e) {
        Log.i(TAG, "error: uri=" + uri + ";e=" + e);
    }
}).navigation(this);

```

startActivityForResult 跳转结果回调

```
Router.getInstance().build("activity/two").navigation(this, new Callback() {
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: requestCode=" + requestCode + ";resultCode=" + resultCode + ";data=" + data);
    }
});

```

---

## 原理说明

实现一个 Router 框架，涉及到的主要的知识点如下：
1. 注解的处理
2. 怎样解决多个 module 之间的依赖问题，以及如何支持多 module 使用
3. router 跳转及 activty startActivityForResult 的处理


我们带着这三个问题，一起来探索一下。

总共分为四个部分,router-annotion, router-compiler,router-api,stub


![](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_09/20191123160207.png)

router-annotion 主要是定义注解的，用来存放注解文件

router-compiler 主要是用来处理注解的，自动帮我们生成代码

router-api 是对外的 api，用来处理跳转的。

stub 这个是存放一些空的 java 文件，提前占坑。不会打包进 jar。

## router-annotion 

主要定义了三个注解

```
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Route {
    String path();
}
```

```
@Retention(RetentionPolicy.CLASS)
public @interface Modules {
    String[] value();
}

```

```
@Retention(RetentionPolicy.CLASS)
public @interface Module {
    String value();
}
```

Route 注解主要是用来注明跳转的 path 的。

Modules 注解，注明总共有多少个 moudle。

Module 注解，注明当前 moudle 的名字。

Modules，Module 注解主要是为了解决支持多 module 使用的。

---

## router-compiler

router-compiler 只有一个类 RouterProcessor，他的原理其实也是比较简单的，扫描那些类用到注解，并将这些信息存起来，做相应的处理。这里是会生成相应的 java 文件。

主要包括以下两个步骤
1. 根据是否有 `@Modules` `@Module` 注解，然后生成相应的 `RouterInit` 文件
2. 扫描 `@Route` 注解，并根据 `moudleName` 生成相应的 java 文件

### 注解基本介绍

在讲解 RouterProcessor 之前，我们先来了解一下注解的基本知识。

如果对于自定义注解还不熟悉的话，可以先看我之前写的这两篇文章。[Android 自定义编译时注解1 - 简单的例子](https://xujun.blog.csdn.net/article/details/70244169)，[Android 编译时注解 —— 语法详解](https://blog.csdn.net/gdutxiaoxu/article/details/70822023)


```
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

   
}

```
 
 
 首先我们先来看一下 `getSupportedAnnotationTypes` 方法,这个方法返回的是我们支持扫描的注解。
 
 ### 注解的处理
 
 接下来我们再一起来看一下 `process` 方法
 
 ```
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
    } else if (!hasModule) { // 没有使用 @Modules 注解，并且有使用 @Module，生成相应的 RouterInit 文件，使用与单个 moudle
        debug("generate default RouterInit");
        generateDefaultRouterInit();
    }

    // 扫描 Route 注解
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

```

，首先判断是否有注解需要处理，没有的话直接返回 `annotations == null || annotations.size() == 0` 。
 
 接着我们会判断是否有 `@Modules` 注解（这种情况是多个 moudle 使用），有的话会调用 `generateModulesRouterInit(String[] moduleNames)` 方法生成 RouterInit java 文件，当没有 `@Modules` 注解，并且没有 `@Module` （这种情况是单个 moudle 使用），会生成默认的 RouterInit 文件。
 
 ```
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

```

假设说我们有"app","moudle1" 两个 moudle，那么我们最终生成的代码是这样的。

```
public final class RouterInit {
  public static final void init() {
    RouterMapping_app.map();
    RouterMapping_moudle1.map();
  }
}
```

如果我们都没有使用 @Moudles 和 @Module 注解，那么生成的 RouterInit 文件大概是这样的。

```
public final class RouterInit {
  public static final void init() {
    RouterMapping.map();
  }
}
```

这也就是为什么有 stub module 的原因。因为默认情况下，我们需要借助 RouterInit 去初始化 map。如果没有这两个文件，ide 编辑器 在 compile 的时候就会报错。


```
compileOnly project(path: ':stub')
```

![](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_09/20191123173843.png)


我们引入的方式是使用 compileOnly，这样的话再生成 jar 的时候，不会包括这两个文件，但是可以在 ide 编辑器中运行。这也是一个小技巧。




### Route 注解的处理

我们回过来看 process 方法连对 Route 注解的处理。

```
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
```

首先会扫描所有的 Route 注解，并添加到 targetInfos list 当中，接着调用 `generateCode` 方法生成相应的文件。

 
```
private void generateCode(List<TargetInfo> targetInfos, String moduleName) {
      
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("map")
//                .addAnnotation(Override.class)
                .addModifiers(Modifier.STATIC)
                .addModifiers(Modifier.PUBLIC);

//                .addParameter(parameterSpec);
        for (TargetInfo info : targetInfos) {
            methodSpecBuilder.addStatement("com.xj.router.api.Router.getInstance().add($S, $T.class)", info.getRoute(), info.getTypeElement());
        }


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
```

这个方法主要是使用 javapoet 生成 java 文件，关于 javaposet 的使用可以见[官网文档](https://github.com/square/javapoet),生成的 java 文件是这样的。

```
package com.xj.router.impl;

import com.xj.arounterdemo.MainActivity;
import com.xj.arounterdemo.OneActivity;
import com.xj.arounterdemo.TwoActivity;

/**
 * Generated by Router. Do not edit it!
 */
public class RouterMapping_app {
  public static void map() {
    com.xj.router.api.Router.getInstance().add("activity/main", MainActivity.class);
    com.xj.router.api.Router.getInstance().add("activity/one", OneActivity.class);
    com.xj.router.api.Router.getInstance().add("activity/two", TwoActivity.class);
  }
}
```

可以看到我们定义的注解信息，最终都会调用 `Router.getInstance().add()` 方法存放起来。

---


## router-api

这个 module 主要是多外暴露的 api，最主要的一个文件是 Router。

```
public class Router {

    private static final String TAG = "ARouter";

    private static final Router instance = new Router();

    private Map<String, Class<? extends Activity>> routeMap = new HashMap<>();
    private boolean loaded;

    private Router() {
    }

    public static Router getInstance() {
        return instance;
    }

    public void init() {
        if (loaded) {
            return;
        }
        RouterInit.init();
        loaded = true;
    }
}
```

当我们想要初始化  Router 的时候，代用 init 方法即可。 init 方法会先判断是否初始化过，没有初始化过，会调用 RouterInit#init 方法区初始化。

而在 RouterInit#init 中，会调用 RouterMap_{@moduleName}#map 方法初始化，改方法又调用 `Router.getInstance().add()` 方法，从而完成初始化

![](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_09/20191126154400.png)

### router 跳转回调


```
public interface RouterCallback {

    /**
     * 在跳转 router 之前
     * @param context
     * @param uri
     * @return
     */
    boolean beforeOpen(Context context, Uri uri);

    /**
     * 在跳转 router 之后
     * @param context
     * @param uri
     */
    void afterOpen(Context context, Uri uri);

    /**
     * 没有找到改 router
     * @param context
     * @param uri
     */
    void notFind(Context context, Uri uri);

    /**
     * 跳转 router 错误
     * @param context
     * @param uri
     * @param e
     */
    void error(Context context, Uri uri, Throwable e);
}
```




```
public void navigation(Activity context, int requestCode, Callback callback) {
    beforeOpen(context);
    boolean isFind = false;
    try {
        Activity activity = (Activity) context;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context.getPackageName(), mActivityName));
        intent.putExtras(mBundle);
        getFragment(activity)
                .setCallback(callback)
                .startActivityForResult(intent, requestCode);
        isFind = true;
    } catch (Exception e) {
        errorOpen(context, e);
        tryToCallNotFind(e, context);
    }

    if (isFind) {
        afterOpen(context);
    }

}

private void tryToCallNotFind(Exception e, Context context) {
    if (e instanceof ClassNotFoundException && mRouterCallback != null) {
        mRouterCallback.notFind(context, mUri);
    }
}



```

主要看 navigation 方法，在跳转 activity 的时候，首先会会调用 
beforeOpen 方法回调 RouterCallback#beforeOpen。接着 catch exception 的时候，如果发生错误，会调用 errorOpen 方法回调 RouterCallback#errorOpen 方法。同时调用 tryToCallNotFind 方法判断是否是 ClassNotFoundException，是的话回调 RouterCallback#notFind。

如果没有发生 eception，会回调 RouterCallback#afterOpen。

### Activity 的 startActivityForResult 回调

可以看到我们的 Router 也是支持 startActivityForResult 的


```
Router.getInstance().build("activity/two").navigation(this, new Callback() {
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: requestCode=" + requestCode + ";resultCode=" + resultCode + ";data=" + data);
    }
});

```

它的实现原理其实很简单，是借助一个空白 fragment 实现的，原理的可以看我之前的这一篇文章。

[Android Fragment 的妙用 - 优雅地申请权限和处理 onActivityResult](https://blog.csdn.net/gdutxiaoxu/article/details/86498647)


---

## 小结

我们的 Router 框架，流程大概是这样的。

![](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_09/20191126145607.png)


![](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_09/20191126154400.png)





---

## 题外话

看了上面的文章，文章一开头提到的三个问题，你懂了吗，欢迎在评论区留言评论。

1. 注解的处理
2. 怎样解决多个 module 之间的依赖问题，以及如何支持多 module 使用
3. router 跳转及 activty startActivityForResult 的处理


其实，现在很多 router 框架都借助 gradle 插件来实现。这样有一个好处，就是在多 moudle 使用的时候，我们只需要 `apply plugin` 就 ok,对外屏蔽了一些细节。但其实，他的原理跟我们上面的原理都是差不多的。

接下来，我也会写 gradle plugin 相关的文章，并借助 gradle 实现 Router 框架。有兴趣的话可以关注我的微信公众号，徐公码字，谢谢。


## 相关文章

[java Type 详解](http://blog.csdn.net/gdutxiaoxu/article/details/68926515)

[java 反射机制详解](http://blog.csdn.net/gdutxiaoxu/article/details/68947735)

[注解使用入门（一）](http://blog.csdn.net/gdutxiaoxu/article/details/52017033)

[Android 自定义编译时注解1 - 简单的例子](http://blog.csdn.net/gdutxiaoxu/article/details/70244169)

[Android 编译时注解 —— 语法详解](http://blog.csdn.net/gdutxiaoxu/article/details/70822023)

[带你读懂 ButterKnife 的源码](http://blog.csdn.net/gdutxiaoxu/article/details/71512754)

[Android Fragment 的妙用 - 优雅地申请权限和处理 onActivityResult](https://blog.csdn.net/gdutxiaoxu/article/details/86498647)

[Android 点九图机制讲解及在聊天气泡中的应用](https://xujun.blog.csdn.net/article/details/100998987)



 
扫一扫，欢迎关注我的微信公众号 **stormjun94（徐公码字）**， 目前是一名程序员，不仅分享 Android开发相关知识，同时还分享技术人成长历程，包括个人总结，职场经验，面试经验等，希望能让你少走一点弯路。
  
![](https://imgconvert.csdnimg.cn/aHR0cHM6Ly91c2VyLWdvbGQtY2RuLnhpdHUuaW8vMjAxOS85LzE4LzE2ZDQ0OGIwNzI4ZGQ3MTY?x-oss-process=image/format,png)