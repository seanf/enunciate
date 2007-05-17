/*
 * Copyright 2006 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.enunciate.modules.xfire_client;

import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.PackageDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.ArrayType;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.TypeMirror;
import freemarker.template.TemplateModelException;
import net.sf.jelly.apt.Context;
import net.sf.jelly.apt.decorations.TypeMirrorDecorator;
import net.sf.jelly.apt.decorations.type.DecoratedTypeMirror;
import org.codehaus.enunciate.contract.jaxb.Accessor;
import org.codehaus.enunciate.contract.jaxb.adapters.Adaptable;
import org.codehaus.enunciate.contract.jaxws.ImplicitChildElement;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Converts a fully-qualified class name to its alternate client fully-qualified class name.
 *
 * @author Ryan Heaton
 */
public class ClientClassnameForMethod extends ClientPackageForMethod {

  private boolean jdk15 = false;

  public ClientClassnameForMethod(Map<String, String> conversions) {
    super(conversions);
  }


  @Override
  protected String convertUnwrappedObject(Object unwrapped) throws TemplateModelException {
    if (unwrapped instanceof Accessor) {
      return convert((Accessor) unwrapped);
    }
    else if (unwrapped instanceof ImplicitChildElement) {
      return convert((ImplicitChildElement) unwrapped);
    }
    else {
      return super.convertUnwrappedObject(unwrapped);
    }
  }

  /**
   * Converts the specified implicit child element.
   *
   * @param childElement The implicit child element.
   * @return The conversion.
   */
  protected String convert(ImplicitChildElement childElement) throws TemplateModelException {
    TypeMirror elementType = childElement.getType();
    if (!isJdk15()) {
      boolean isArray = elementType instanceof ArrayType;
      if (isArray || !((DecoratedTypeMirror) TypeMirrorDecorator.decorate(elementType)).isCollection()) {
        if ((childElement instanceof Adaptable) && (((Adaptable) childElement).isAdapted())) {
          elementType = (((Adaptable) childElement).getAdapterType().getAdaptingType());

          if (isArray) {
            //the adapting type adapts the component, so we need to convert it back to an array type.
            AnnotationProcessorEnvironment ape = Context.getCurrentEnvironment();
            elementType = ape.getTypeUtils().getArrayType(elementType);
          }
        }
      }
    }

    return convert(elementType);
  }

  /**
   * Converts the type of an accessor.
   *
   * @param accessor The accessor.
   * @return The accessor.
   */
  protected String convert(Accessor accessor) throws TemplateModelException {
    TypeMirror accessorType = accessor.getAccessorType();
    if (!isJdk15()) {
      boolean isArray = accessorType instanceof ArrayType;
      if (isArray || !accessor.isCollectionType()) { //we don't care about collections because jdk 14 doesn't have generics.
        if (accessor.isAdapted()) {
          accessorType = accessor.getAdapterType().getAdaptingType();
          
          if (isArray) {
            //the adapting type will adapt the component, so we need to convert it back to an array type.
            AnnotationProcessorEnvironment ape = Context.getCurrentEnvironment();
            accessorType = ape.getTypeUtils().getArrayType(accessorType);
          }
        }
      }
    }

    return convert(accessorType);
  }

  @Override
  protected String convert(TypeMirror typeMirror) throws TemplateModelException {
    boolean isArray = typeMirror instanceof ArrayType;
    String conversion = super.convert(typeMirror);

    //if we're using converting to a java 5+ client code, take into account the type arguments.
    if ((isJdk15()) && (typeMirror instanceof DeclaredType)) {
      DeclaredType declaredType = (DeclaredType) typeMirror;
      Collection<TypeMirror> actualTypeArguments = declaredType.getActualTypeArguments();
      if (actualTypeArguments.size() > 0) {
        StringBuilder typeArgs = new StringBuilder("<");
        Iterator<TypeMirror> it = actualTypeArguments.iterator();
        while (it.hasNext()) {
          TypeMirror mirror = it.next();
          typeArgs.append(convert(mirror));
          if (it.hasNext()) {
            typeArgs.append(", ");
          }
        }
        typeArgs.append(">");
        conversion += typeArgs;
      }
    }

    if (isArray) {
      conversion += "[]";
    }
    return conversion;

  }

  @Override
  protected String convert(TypeDeclaration declaration) {
    String convertedPackage;
    PackageDeclaration pckg = declaration.getPackage();
    if (pckg == null) {
      convertedPackage = "";
    }
    else {
      convertedPackage = super.convert(pckg.getQualifiedName());
    }

    return convertedPackage + "." + declaration.getSimpleName();
  }

  @Override
  protected String convert(PackageDeclaration packageDeclaration) {
    throw new UnsupportedOperationException("packages don't have a client classname.");
  }

  /**
   * Whether this converter is enabled to output jdk 15 compatible classes.
   *
   * @return Whether this converter is enabled to output jdk 15 compatible classes.
   */
  public boolean isJdk15() {
    return jdk15;
  }

  /**
   * Whether this converter is enabled to output jdk 15 compatible classes.
   *
   * @param jdk15 Whether this converter is enabled to output jdk 15 compatible classes.
   */
  public void setJdk15(boolean jdk15) {
    this.jdk15 = jdk15;
  }
}
