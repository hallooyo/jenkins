package hudson.model;

import hudson.model.Descriptor.FormException;
import hudson.util.CaseInsensitiveComparator;
import hudson.Indenter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.text.ParseException;

/**
 *
 * <h2>EXPERIMENTAL</h2>
 * <p>
 * The intention is to eventually merge this with the {@link ListView}.
 * This class is here for experimentation.
 *
 * <p>
 * Until this class is sufficiently stable, there's no need for l10n.
 *
 * @author Kohsuke Kawaguchi
 */
public class TreeView extends View implements ViewGroup {
    /**
     * List of job names. This is what gets serialized.
     */
    private final Set<String> jobNames
        = new TreeSet<String>(CaseInsensitiveComparator.INSTANCE);

    /**
     * Nested views.
     */
    private final CopyOnWriteArrayList<View> views = new CopyOnWriteArrayList<View>();

    @DataBoundConstructor
    public TreeView(String name) {
        super(name);
    }

    /**
     * Returns {@link Indenter} that has the fixed indentation width.
     * Used for assisting view rendering.
     */
    public Indenter createFixedIndenter(String d) {
        final int depth = Integer.parseInt(d);
        return new Indenter() {
            protected int getNestLevel(Job job) { return depth; }
        };
    }

    /**
     * Returns a read-only view of all {@link Job}s in this view.
     *
     * <p>
     * This method returns a separate copy each time to avoid
     * concurrent modification issue.
     */
    public synchronized List<TopLevelItem> getItems() {
        return Hudson.getInstance().getItems();
//        List<TopLevelItem> items = new ArrayList<TopLevelItem>(jobNames.size());
//        for (String name : jobNames) {
//            TopLevelItem item = Hudson.getInstance().getItem(name);
//            if(item!=null)
//                items.add(item);
//        }
//        return items;
    }

    public boolean contains(TopLevelItem item) {
        return true;
//        return jobNames.contains(item.getName());
    }

    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        Item item = Hudson.getInstance().doCreateItem(req, rsp);
        if(item!=null) {
            jobNames.add(item.getName());
            owner.save();
        }
        return item;
    }

    @Override
    public synchronized void onJobRenamed(Item item, String oldName, String newName) {
        if(jobNames.remove(oldName) && newName!=null)
            jobNames.add(newName);
        // forward to children
        for (View v : views)
            v.onJobRenamed(item,oldName,newName);
    }

    protected void submit(StaplerRequest req) throws IOException, ServletException, FormException {
    }

    public void deleteView(View view) throws IOException {
        views.remove(view);
    }

    public Collection<View> getViews() {
        return Collections.unmodifiableList(views);
    }

    public View getView(String name) {
        for (View v : views)
            if(v.getViewName().equals(name))
                return v;
        return null;
    }

    public void onViewRenamed(View view, String oldName, String newName) {
        // noop
    }

    public void save() throws IOException {
        owner.save();
    }

    public void doCreateView( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        try {
            checkPermission(View.CREATE);
            views.add(View.create(req,rsp,this));
            save();
        } catch (ParseException e) {
            sendError(e,req,rsp);
        } catch (FormException e) {
            sendError(e,req,rsp);
        }
    }

    public ViewDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    static {
        LIST.add(DESCRIPTOR);
    }

    public static final class DescriptorImpl extends ViewDescriptor {
        private DescriptorImpl() {
            super(TreeView.class);
        }

        public String getDisplayName() {
            return "Tree View";
        }
    }
}
