package jadetree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import objectattributes.NominalObjectAttribute;
import objectattributes.ObjectAttribute;
import utils.HolderBase;
import utils.Predicate;

/**
 *
 * @author kommusoft
 * @param <TSource> The source of the types to classify.
 */
public class ClassificationTree<TSource> {

    private static final Logger LOG = Logger.getLogger(ClassificationTree.class.getName());

    private final ArrayList<ObjectAttribute<? super TSource, ?>> sourceAttributes = new ArrayList<>();
    NominalObjectAttribute<? super TSource, ?> targetAttribute;
    private DecisionNode root = new DecisionLeaf();

    public void addSourceAttribute(ObjectAttribute<? super TSource, ?> sourceAttribute) {
        this.sourceAttributes.add(sourceAttribute);
        this.root.makeDirty();
    }

    public void removeSourceAttribute(ObjectAttribute<? super TSource, ?> sourceAttribute) {
        this.sourceAttributes.remove(sourceAttribute);
        this.root.makeDirty();
    }

    public void insert(TSource element) {
        this.root.insert(element);
    }

    public void reduceMemory() {
        this.root.makeDirty();
    }

    public class DecisionTreeNode<TTarget> {

        ObjectAttribute<? super TSource, TTarget> decisionattribute;

    }

    public abstract class DecisionNode {

        public boolean isLeaf() {
            return false;
        }

        public DecisionNode nextHop(TSource source) {
            return this;
        }

        public abstract double expandScore();

        public void insert(TSource source) {
            this.nextHop(source).insert(source);
        }

        public abstract void makeDirty();

        public abstract DecisionLeaf getMaximumLeaf();

    }

    public abstract class DecisionInode extends DecisionNode {

        private DecisionLeaf maximumLeaf = null;

        protected DecisionInode() {
        }

        @Override
        public double expandScore() {
            return this.getMaximumLeaf().expandScore();
        }

        @Override
        public DecisionLeaf getMaximumLeaf() {
            if (this.maximumLeaf == null) {
                this.maximumLeaf = this.recalcMaximumLeaf();
            }
            return this.maximumLeaf;
        }

        @Override
        public void makeDirty() {
            this.maximumLeaf = null;
        }

        protected abstract DecisionLeaf recalcMaximumLeaf();

    }

    public abstract class AttributeDecisionNode extends DecisionInode {

        private final ObjectAttribute<? super TSource, ?> objectAttribute;

        protected AttributeDecisionNode(ObjectAttribute<? super TSource, ?> objectAttribute) {
            this.objectAttribute = objectAttribute;
        }

        @Override
        public double expandScore() {
            return this.getMaximumLeaf().expandScore();
        }

        /**
         * @return the objectAttribute
         */
        protected ObjectAttribute<? super TSource, ?> getObjectAttribute() {
            return objectAttribute;
        }

        protected Object getObjectAttribute(TSource source) {
            return this.objectAttribute.evaluate(source);
        }

    }

    public class PredicateDecisionNode extends DecisionInode {

        private final Predicate<? super TSource> predicate;
        private DecisionNode trueNode;
        private DecisionNode falseNode;

        public PredicateDecisionNode(Predicate<? super TSource> predicate, DecisionNode trueNode, DecisionNode falseNode) {
            this.predicate = predicate;
            this.trueNode = trueNode;
            this.falseNode = falseNode;
        }

        public PredicateDecisionNode(Predicate<? super TSource> predicate) {
            this(predicate, new DecisionLeaf(), new DecisionLeaf());
        }

        @Override
        public DecisionNode nextHop(TSource source) {
            if (this.getPredicate().evaluate(source)) {
                return this.getTrueNode();
            } else {
                return this.getFalseNode();
            }
        }

        @Override
        public void makeDirty() {
            this.getTrueNode().makeDirty();
            this.getFalseNode().makeDirty();
            super.makeDirty();
        }

        @Override
        protected DecisionLeaf recalcMaximumLeaf() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        /**
         * @return the predicate
         */
        public Predicate<? super TSource> getPredicate() {
            return predicate;
        }

        /**
         * @return the trueNode
         */
        public DecisionNode getTrueNode() {
            return trueNode;
        }

        /**
         * @param trueNode the trueNode to set
         */
        public void setTrueNode(DecisionNode trueNode) {
            this.trueNode = trueNode;
        }

        /**
         * @return the falseNode
         */
        public DecisionNode getFalseNode() {
            return falseNode;
        }

        /**
         * @param falseNode the falseNode to set
         */
        public void setFalseNode(DecisionNode falseNode) {
            this.falseNode = falseNode;
        }

    }

    public class EnumerableDecisionNode extends AttributeDecisionNode {

        private final HashMap<Object, DecisionNode> map = new HashMap<>();

        protected EnumerableDecisionNode(ObjectAttribute<? super TSource, ?> objectAttribute) {
            super(objectAttribute);
        }

        @Override
        public DecisionNode nextHop(TSource source) {
            Object key = this.getObjectAttribute(source);
            DecisionNode value = map.get(key);
            if (value == null) {
                value = new DecisionLeaf();
                this.map.put(key, value);
            }
            return value;
        }

        @Override
        public void makeDirty() {
            for (DecisionNode dn : this.map.values()) {
                dn.makeDirty();
            }
            super.makeDirty();
        }

        @Override
        protected DecisionLeaf recalcMaximumLeaf() {
            double max = Double.NEGATIVE_INFINITY, val;
            DecisionLeaf leaf, maxLeaf = null;
            for (DecisionNode dn : this.map.values()) {
                leaf = dn.getMaximumLeaf();
                val = leaf.expandScore();
                if (val > max) {
                    max = val;
                    maxLeaf = leaf;
                }
            }
            return maxLeaf;
        }

    }

    public class DecisionLeaf extends DecisionNode {

        private final ArrayList<TSource> memory = new ArrayList<>();
        private double score = Double.NaN;
        private int splitIndex = 0x00;
        private final HolderBase<Object> splitData = new HolderBase<>();

        public boolean isDirty() {
            return Double.isNaN(this.score);
        }

        @Override
        public void makeDirty() {
            this.score = Double.NaN;
            splitData.setData(null);
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public double expandScore() {
            if (this.isDirty()) {
                this.score = this.calculateScore();
            }
            return this.score;
        }

        public DecisionNode expand() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void insert(TSource source) {
            this.makeDirty();
            memory.add(source);
        }

        private double calculateScore() {
            double maxScore = Double.NEGATIVE_INFINITY;
            int maxIndex = -0x01, i = 0x00;
            HolderBase<Object> curData = new HolderBase<>();
            for (ObjectAttribute<? super TSource, ?> oa : ClassificationTree.this.sourceAttributes) {
                double sc = oa.calculateScore(this.memory, curData);
                if (sc > maxScore) {
                    maxScore = sc;
                    maxIndex = i;
                    this.splitData.copyFrom(curData);
                }
                i++;
            }
            this.splitIndex = maxIndex;
            return maxScore;
        }

        @Override
        public DecisionLeaf getMaximumLeaf() {
            return this;
        }

    }

}
