package net.sf.enunciate.contract.jaxws;

import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.TypeVisitor;
import net.sf.enunciate.apt.EnunciateFreemarkerModel;
import net.sf.enunciate.contract.jaxb.RootElementDeclaration;
import net.sf.enunciate.contract.jaxb.types.XmlTypeException;
import net.sf.enunciate.contract.jaxb.types.XmlTypeMirror;
import net.sf.enunciate.contract.validation.ValidationException;
import net.sf.jelly.apt.decorations.type.DecoratedTypeMirror;
import net.sf.jelly.apt.freemarker.FreemarkerModel;

import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A decorated type mirror that is a web result.
 *
 * @author Ryan Heaton
 */
public class WebResult extends DecoratedTypeMirror implements WebMessage, WebMessagePart, ImplicitChildElement {

  private final boolean header;
  private final String name;
  private final String targetNamespace;
  private final String partName;
  private final WebMethod method;

  protected WebResult(TypeMirror delegate, WebMethod method) {
    super(delegate);
    this.method = method;

    javax.jws.WebResult annotation = method.getAnnotation(javax.jws.WebResult.class);

    String name = "return";
    if (annotation != null) {
      name = annotation.name();
    }
    this.name = name;

    String targetNamespace = method.getDeclaringEndpointInterface().getTargetNamespace();
    if ((annotation != null) && (annotation.targetNamespace() != null) && (!"".equals(annotation.targetNamespace()))) {
      targetNamespace = annotation.targetNamespace();
    }
    this.targetNamespace = targetNamespace;

    String partName = "return";
    if ((annotation != null) && (!"".equals(annotation.partName()))) {
      partName = annotation.partName();
    }
    this.partName = partName;
    this.header = ((annotation != null) && (annotation.header()));
  }

  public void accept(TypeVisitor typeVisitor) {
    delegate.accept(typeVisitor);
  }

  /**
   * The name of the web result.
   *
   * @return The name of the web result.
   */
  public String getName() {
    return name;
  }

  /**
   * The namespace of the web result.
   *
   * @return The namespace of the web result.
   */
  public String getTargetNamespace() {
    return targetNamespace;
  }

  /**
   * The part name.
   *
   * @return The part name.
   */
  public String getPartName() {
    return partName;
  }

  /**
   * The web method.
   *
   * @return The web method.
   */
  public WebMethod getWebMethod() {
    return method;
  }

  /**
   * Get the delegate.
   *
   * @return The delegate.
   */
  public TypeMirror getDelegate() {
    return this.delegate;
  }

  /**
   * Whether this is a bare web result.
   *
   * @return Whether this is a bare web result.
   */
  private boolean isBare() {
    return method.getSoapParameterStyle() == SOAPBinding.ParameterStyle.BARE;
  }

  /**
   * The message name in the case of a document/bare service.
   *
   * @return The message name in the case of a document/bare service.
   */
  public String getMessageName() {
    return method.getResponseMessageName();
  }

  /**
   * There is only message documentation if this web result is BARE.
   *
   * @return The documentation if BARE, null otherwise.
   */
  public String getMessageDocs() {
    if (isBare()) {
      return getDocComment();
    }

    return null;
  }

  // Inherited.
  public boolean isInput() {
    return false;
  }

  // Inherited.
  public boolean isOutput() {
    return true;
  }

  // Inherited.
  public boolean isHeader() {
    return header;
  }

  // Inherited.
  public boolean isFault() {
    return false;
  }

  /**
   * If this web result is a part, the comments for the result.
   *
   * @return The part docs.
   */
  public String getPartDocs() {
    if (isBare()) {
      return null;
    }

    return getDocComment();
  }

  /**
   * The qname of the element for this web result as a part.
   *
   * @return The qname of the element for this web result as a part.
   */
  public QName getElementQName() {
    TypeMirror returnType = getDelegate();
    if (returnType instanceof DeclaredType) {
      TypeDeclaration returnTypeDeclaration = ((DeclaredType) returnType).getDeclaration();
      if ((returnTypeDeclaration instanceof ClassDeclaration) && (returnTypeDeclaration.getAnnotation(XmlRootElement.class) != null)) {
        EnunciateFreemarkerModel model = ((EnunciateFreemarkerModel) FreemarkerModel.get());
        RootElementDeclaration rootElement = model.findRootElementDeclaration((ClassDeclaration) returnTypeDeclaration);
        if (rootElement == null) {
          throw new ValidationException(method.getPosition(), returnTypeDeclaration.getQualifiedName() +
            " is not a known root element.  Please add it to the list of known classes.");
        }

        return new QName(rootElement.getQualifiedName(), rootElement.getName());
      }
    }

    return new QName(method.getDeclaringEndpointInterface().getTargetNamespace(), getElementName());
  }

  /**
   * This web result defines an implicit schema element if it is NOT of a class type that is an xml root element.
   *
   * @return Whether this web result is an implicit schema element.
   */
  public boolean isImplicitSchemaElement() {
    TypeMirror returnType = getDelegate();
    return !((returnType instanceof DeclaredType) && (((DeclaredType) returnType).getDeclaration().getAnnotation(XmlRootElement.class) != null));
  }

  // Inherited.
  public Collection<WebMessagePart> getParts() {
    if (!isBare()) {
      throw new UnsupportedOperationException("Web result doesn't represent a complex method input/output.");
    }

    return new ArrayList<WebMessagePart>(Arrays.asList(this));
  }

  /**
   * The qname of the type of this result as an implicit schema element.
   *
   * @return The qname of the type of this result.
   * @throws ValidationException If the type is anonymous or otherwise problematic.
   */
  public QName getTypeQName() {
    try {
      EnunciateFreemarkerModel model = ((EnunciateFreemarkerModel) FreemarkerModel.get());
      XmlTypeMirror xmlType = model.getXmlType(getDelegate());
      if (xmlType.isAnonymous()) {
        throw new ValidationException(method.getPosition(), "Type of web result cannot be anonymous.");
      }

      return xmlType.getQname();
    }
    catch (XmlTypeException e) {
      throw new ValidationException(method.getPosition(), e.getMessage());
    }
  }

  public int getMinOccurs() {
    return isPrimitive() ? 1 : 0;
  }

  public String getMaxOccurs() {
    return isArray() || isCollection() ? "unbounded" : "1";
  }

  public String getElementName() {
    return getName();
  }

  public String getElementDocs() {
    return getDocComment();
  }


}