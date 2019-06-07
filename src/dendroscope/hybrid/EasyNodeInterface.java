/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import java.util.Vector;

public interface EasyNodeInterface {

    void delete();

    Vector<EasyNode> getChildren();

    EasyNode getParent();

    String getLabel();

    EasyTree getOwner();

    int getOutDegree();

    int getInDegree();

    void addChild(EasyNode v);

    void removeChild(EasyNode v);

    void restrict();

}
