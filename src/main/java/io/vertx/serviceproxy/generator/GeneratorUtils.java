package io.vertx.serviceproxy.generator;

import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.ClassTypeInfo;
import io.vertx.codegen.type.DataObjectTypeInfo;
import io.vertx.codegen.type.MapperInfo;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.serviceproxy.generator.model.ProxyModel;

import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.stream.Stream;

/**
 * @author <a href="http://slinkydeveloper.github.io">Francesco Guardiani @slinkydeveloper</a>
 */
public class GeneratorUtils {

  final String classHeader;
  final String proxyGenImports;
  final String handlerGenImports;
  final String roger;
  final String handlerConstructorBody;
  final String handlerCloseAccessed;

  public GeneratorUtils() {
    classHeader = loadResource("class_header") + "\n";
    proxyGenImports = loadResource("proxy_gen_import") + "\n";
    handlerGenImports = loadResource("handler_gen_import") + "\n";
    handlerConstructorBody = loadResource("handler_constructor_body") + "\n";
    handlerCloseAccessed = loadResource("handler_close_accessed") + "\n";
    roger = loadResource("roger") + "\n";
  }

  public Stream<String> additionalImports(ProxyModel model) {
    return Stream
      .concat(
        model.getImportedTypes().stream(),
        model.getReferencedDataObjectTypes()
          .stream()
          .filter(t -> t.getTargetType() instanceof ClassTypeInfo)
          .map(t -> (ClassTypeInfo) t.getTargetType())
      )
      .filter(c -> !c.getPackageName().equals("java.lang") && !c.getPackageName().equals("io.vertx.core.json"))
      .map(ClassTypeInfo::toString)
      .distinct();
  }

  public void classHeader(PrintWriter w) {
    w.print(classHeader);
  }

  public void proxyGenImports(PrintWriter w) {
    w.print(proxyGenImports);
  }

  public void handlerGenImports(PrintWriter w) { w.print(handlerGenImports); }

  public void roger(PrintWriter w) { w.print(roger); }

  public void handlerConstructorBody(PrintWriter w) { w.print(handlerConstructorBody); }

  public void handleCloseAccessed(PrintWriter w) { w.print(handlerCloseAccessed); }

  public void writeImport(PrintWriter w, String i) {
    w.print("import " + i + ";\n");
  }

  public String loadResource(String resource) {
    return loadResource(resource, "vertx-service-proxy");
  }

  public String loadResource(String resource, String moduleName) {
    InputStream input = GeneratorUtils.class.getResourceAsStream("/META-INF/vertx/" + moduleName + "/" + resource + ".txt");
    try (Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name())) {
      return scanner.useDelimiter("\\A").next();
    }
  }

  public boolean isResultHandler(ParamInfo param) {
    return param != null &&
      param.getType().getKind() == ClassKind.HANDLER &&
      ((ParameterizedTypeInfo)param.getType()).getArg(0).getKind() == ClassKind.ASYNC_RESULT;
  }

  public static String generateDeserializeDataObject(String stmt, DataObjectTypeInfo doTypeInfo) {
    MapperInfo deserializer = doTypeInfo.getDeserializer();
    String s;
    switch (deserializer.getKind()) {
      case SELF:
        s = String.format("new %s((%s)%s)", doTypeInfo.getName(), doTypeInfo.getTargetType().getSimpleName(), stmt);
        break;
      case FUNCTION:
        s =  String.format("%s.%s.apply((%s)%s)", deserializer.getQualifiedName(), deserializer.getName(), deserializer.getTargetType().getSimpleName(), stmt);
        break;
      case STATIC_METHOD:
        s =  String.format("%s.%s((%s)%s)", deserializer.getQualifiedName(), deserializer.getName(), deserializer.getTargetType().getSimpleName(), stmt);
        break;
      default:
        throw new AssertionError();
    }
    return String.format("%s != null ? %s : null", stmt, s);
  }

  public static String generateSerializeDataObject(String stmt, DataObjectTypeInfo doTypeInfo) {
    MapperInfo serializer = doTypeInfo.getSerializer();
    String s;
    switch (serializer.getKind()) {
      case SELF:
        s = String.format("%s.toJson()", stmt);
        break;
      case FUNCTION:
        s = String.format("%s.%s.apply(%s)", serializer.getQualifiedName(), serializer.getName(), stmt);
        break;
      case STATIC_METHOD:
        s = String.format("%s.%s(%s)", serializer.getQualifiedName(), serializer.getName(), stmt);
        break;
      default:
        throw new AssertionError();
    }
    return String.format("%s != null ? %s : null", stmt, s);
  }
}
