/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package dendroscope.hybrid;

import java.util.Vector;

public class EasyNode implements EasyNodeInterface {

    private final Vector<EasyNode> children = new Vector<>();

    private EasyNode parent;
    private EasyTree owner;
    private String label;

    public EasyNode(EasyNode parent, EasyTree owner, String label) {
        this.parent = parent;
        this.owner = owner;
        this.label = label;
        if (owner != null) {
            owner.addLeaf(this);
            owner.setTreeChanged(true);
        }
        if (parent != null)
            this.parent.addChild(this);
        if (owner != null && parent != null) {
            if (owner.getLeaves().contains(parent)) {
                owner.removeLeaf(parent);
                owner.setTreeChanged(true);
            }
        }
    }

    public Vector<EasyNode> getChildren() {
        return children;
    }

    public EasyNode getParent() {
        return parent;
    }

    public String getLabel() {
        return label;
    }

    public EasyTree getOwner() {
        return owner;
    }

    public int getOutDegree() {
        return children.size();
    }

    public int getInDegree() {
        if (parent != null)
            return 1;
        return 0;
    }

    public void addChild(EasyNode v) {
        children.add(v);
        v.setParent(this);
    }

    public void removeChild(EasyNode v) {
        children.remove(v);
        v.setParent(null);
    }

    public void delete() {
        if (children.size() == 0)
            owner.removeLeaf(this);
        for (EasyNode c : children)
            c.delete();
    }

    public void setLabel(String s) {
        label = s;
    }

    public void setParent(EasyNode p) {
        parent = p;
    }

    public void restrict() {
        if (parent != null) {
            EasyNode p = parent;
            p.removeChild(this);
            for (EasyNode c : children) {
                p.addChild(c);
                c.setParent(p);
            }
            if (p.getOutDegree() == 0)
                owner.addLeaf(p);
        } else if (children.size() == 1) {
            children.firstElement().setParent(null);
            owner.setRoot(children.firstElement());
        }
    }

    public void setOwner(EasyTree t) {
        owner = t;
    }

}
