package jahmm.jadetree;

/**
 *
 * @author kommusoft
 * @param <TSource>
 */
public abstract class DecisionInode<TSource> extends DecisionNodeBase<TSource> {

    private DecisionLeaf<TSource> maximumLeaf = null;

    protected DecisionInode(DecisionNode<TSource> parent) {
        super(parent);
    }

    @Override
    public double expandScore() {
        return this.getMaximumLeaf().expandScore();
    }

    @Override
    public DecisionLeaf<TSource> getMaximumLeaf() {
        if (this.maximumLeaf == null) {
            this.maximumLeaf = this.recalcMaximumLeaf();
        }
        return this.maximumLeaf;
    }

    @Override
    public void makeDirty() {
        this.maximumLeaf = null;
    }

    protected abstract DecisionLeaf<TSource> recalcMaximumLeaf();

}