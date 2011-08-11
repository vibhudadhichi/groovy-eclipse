/*
 * Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.preferencepage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.GroovyMethodSuggestion;
import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.GroovyPropertySuggestion;
import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.GroovySuggestionDeclaringType;
import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.IGroovySuggestion;
import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.InferencingSuggestionsManager;
import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.SuggestionDescriptor;
import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.ui.IProjectUIControl;
import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.ui.ISelectionHandler;
import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.ui.InferencingContributionDialogue;
import org.codehaus.groovy.eclipse.dsl.inferencing.suggestions.ui.ProjectDropDownControl;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * 
 * @author Nieraj Singh
 * @created Apr 19, 2011
 */
public class GroovySuggestionsTable {

    private static final String DEACTIVATE_REMOVE_EDIT_OR_ADD_A_TYPE_SUGGESTION = "Deactivate, remove, edit, or add a type suggestion:";

    private Map<ButtonTypes, Button> selectionButtons;

    private List<IProject> projects;

    private IProjectUIControl selector;

    private SuggestionsViewer viewer;

    enum ButtonTypes {
        EDIT("Edit..."),

        ADD("Add..."),

        REMOVE("Remove"),

        SELECT_ALL("Select All"),

        DESELECT_ALL("Deselect All"),

        COLLAPSE_ALL("Collapse All"),

        EXPAND_ALL("Expand All");

        private String label;

        private ButtonTypes(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

    }

    enum ColumnTypes implements ITreeViewerColumn {
        SUGGESTIONS("Suggestions", 60);

        private String label;

        private int weight;

        private ColumnTypes(String label, int weight) {
            this.label = label;
            this.weight = weight;
        }

        public String getName() {
            return label;
        }

        public int getWidth() {
            return weight;
        }

    }

    public GroovySuggestionsTable(List<IProject> projects) {
        this.projects = projects != null ? projects : new ArrayList<IProject>();
    }

    public Composite createTable(Composite parent) {
        Composite subparent = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).applyTo(subparent);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
        createProjectArea(subparent);

        return createViewerArea(subparent);
    }

    protected void createProjectArea(Composite parent) {

        Composite subparent = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).applyTo(subparent);
        GridDataFactory.fillDefaults().grab(false, false).applyTo(parent);
        ISelectionHandler handler = new ISelectionHandler() {

            public void selectionChanged(IProject project) {
                setViewerInput(project);
            }
        };
        selector = new ProjectDropDownControl(projects, parent.getShell(), subparent, handler);
        selector.createControls();

    }

    protected String getViewerLabel() {
        return DEACTIVATE_REMOVE_EDIT_OR_ADD_A_TYPE_SUGGESTION;
    }

    protected IProject getSelectedProject() {
        if (selector == null) {
            return null;
        }
        return selector.getProject();
    }

    protected Composite createViewerArea(Composite parent) {
        String label = getViewerLabel();
        if (label != null && label.length() > 0) {
            Label viewerLabel = new Label(parent, SWT.READ_ONLY);

            viewerLabel.setText(label);

            GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.CENTER).applyTo(viewerLabel);
        }

        Composite subparent = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).applyTo(subparent);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);

        createTableViewer(subparent);

        createOperationButtonArea(subparent);
        return subparent;
    }

    protected void collapseAll() {
        viewer.getTreeViewer().collapseAll();
    }

    protected void expandAll() {
        viewer.getTreeViewer().expandAll();
    }

    protected void uncheckAll() {
        setCheckStateAll(false);
    }

    protected void checkAll() {
        setCheckStateAll(true);
    }

    protected void setCheckStateAll(boolean checkState) {
        IProject project = getSelectedProject();
        Collection<GroovySuggestionDeclaringType> declaringTypes = InferencingSuggestionsManager.getInstance()
                .getSuggestions(project).getDeclaringTypes();
        for (GroovySuggestionDeclaringType declaringType : declaringTypes) {
            // Set active state in the model
            setActiveState(declaringType, checkState);

            refresh();
            // update the viewer check state
            setCheckState(declaringType);
        }

    }

    protected ITreeViewerColumn[] getColumns() {
        return ColumnTypes.values();
    }

    protected void createTableViewer(Composite parent) {
        viewer = new SuggestionsViewer(getColumns(), ColumnTypes.SUGGESTIONS);
        viewer.createControls(parent);
        final CheckboxTreeViewer treeViewer = viewer.getTreeViewer();

        treeViewer.addCheckStateListener(new ICheckStateListener() {

            public void checkStateChanged(CheckStateChangedEvent event) {
                Object obj = event.getElement();
                setActiveState(obj, event.getChecked());
            }
        });

        treeViewer.addTreeListener(new ITreeViewerListener() {

            public void treeExpanded(TreeExpansionEvent event) {
                setCheckState(event.getElement());
            }

            public void treeCollapsed(TreeExpansionEvent event) {
                // do nothing
            }
        });

        treeViewer.setLabelProvider(new ViewerLabelProvider());
        treeViewer.setContentProvider(new ViewerContentProvider());
        treeViewer.setComparator(new SuggestionViewerSorter());
        setViewerListeners(treeViewer);
        setViewerInput(getSelectedProject());

    }

    protected void setActiveState(Object viewerElement, boolean checkState) {
        if (viewerElement instanceof GroovySuggestionDeclaringType) {
            GroovySuggestionDeclaringType declaringType = (GroovySuggestionDeclaringType) viewerElement;
            Set<IGroovySuggestion> suggestions = declaringType.getSuggestions();

            for (IGroovySuggestion suggestion : suggestions) {
                suggestion.changeActiveState(checkState);
            }

        } else if (viewerElement instanceof IGroovySuggestion) {
            IGroovySuggestion suggestion = (IGroovySuggestion) viewerElement;
            suggestion.changeActiveState(checkState);
        }
    }

    /**
     * Sets check state in the viewer for all visible elements. Collapsed
     * elements will get the
     * state set when expanded.
     * 
     * @param viewerElement
     */
    protected void setCheckState(Object viewerElement) {
        // Only set the child state as the parent state is derived from the
        // child and automatically set by the container check box viewer which
        // updates the parent state.
        // For example, if one child of the parent is checked, the parent state
        // is automatically set by the viewer
        // Parent check state does not require manual setting.
        if (viewerElement instanceof GroovySuggestionDeclaringType) {
            GroovySuggestionDeclaringType declaringType = (GroovySuggestionDeclaringType) viewerElement;
            Set<IGroovySuggestion> suggestions = declaringType.getSuggestions();

            for (Iterator<IGroovySuggestion> it = suggestions.iterator(); it.hasNext();) {
                IGroovySuggestion suggestion = it.next();
                boolean isSuggestionActive = suggestion.isActive();
                viewer.getTreeViewer().setChecked(suggestion, isSuggestionActive);
            }

        } else if (viewerElement instanceof IGroovySuggestion) {
            IGroovySuggestion suggestion = (IGroovySuggestion) viewerElement;
            viewer.getTreeViewer().setChecked(suggestion, suggestion.isActive());
        }
    }

    protected void setViewerListeners(TreeViewer tree) {
        tree.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                if (event.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    handleSelectionButtonEnablement(selection.toList());
                }
            }
        });
    }

    protected void handleSelectionButtonEnablement(List<Object> selectedObjects) {
        if (projects == null || projects.isEmpty()) {
            selectionButtons.get(ButtonTypes.ADD).setEnabled(false);
            selectionButtons.get(ButtonTypes.EDIT).setEnabled(false);
            selectionButtons.get(ButtonTypes.REMOVE).setEnabled(false);
        } else if (selectedObjects == null || selectedObjects.isEmpty()) {
            selectionButtons.get(ButtonTypes.ADD).setEnabled(true);
            selectionButtons.get(ButtonTypes.EDIT).setEnabled(false);
            selectionButtons.get(ButtonTypes.REMOVE).setEnabled(false);
        } else if (selectedObjects.size() == 1) {
            Object selectedObj = selectedObjects.get(0);
            if (selectedObj instanceof GroovySuggestionDeclaringType) {
                selectionButtons.get(ButtonTypes.ADD).setEnabled(true);
                selectionButtons.get(ButtonTypes.EDIT).setEnabled(false);
                selectionButtons.get(ButtonTypes.REMOVE).setEnabled(true);
            } else if (selectedObj instanceof IGroovySuggestion) {
                selectionButtons.get(ButtonTypes.ADD).setEnabled(false);
                selectionButtons.get(ButtonTypes.EDIT).setEnabled(true);
                selectionButtons.get(ButtonTypes.REMOVE).setEnabled(true);
            }

        } else {
            selectionButtons.get(ButtonTypes.ADD).setEnabled(false);
            selectionButtons.get(ButtonTypes.EDIT).setEnabled(false);
            selectionButtons.get(ButtonTypes.REMOVE).setEnabled(true);
        }
    }

    /**
     * Dialogue can only be opened on ONE selection or no selections. It will
     * not be opened on multiple selections.
     */
    protected void editSuggestion() {

        if (getSelections().size() > 1) {
            return;
        } else {

            if (getSelections().size() == 1) {
                Object selectedObj = getSelections().get(0);
                if (selectedObj instanceof IGroovySuggestion) {
                    // This edits an existing suggestion. Retain active state.
                    IGroovySuggestion suggestion = (IGroovySuggestion) selectedObj;
                    InferencingContributionDialogue dialogue = new InferencingContributionDialogue(getShell(), suggestion,
                            getSelectedProject());
                    if (dialogue.open() == Window.OK) {
                        SuggestionDescriptor descriptor = dialogue.getSuggestionChange();
                        GroovySuggestionDeclaringType declaringType = suggestion.getDeclaringType();
                        IGroovySuggestion replacedSuggestion = declaringType.replaceSuggestion(descriptor, suggestion);

                        refresh();
                        setCheckState(replacedSuggestion);
                    }
                }
            }
        }
    }

    protected Shell getShell() {
        return viewer != null ? viewer.getTreeViewer().getTree().getShell() : null;
    }

    protected void addSuggestion() {
        if (getSelections().size() > 1) {
            return;
        } else {

            InferencingContributionDialogue dialogue = null;
            if (getSelections().size() == 1) {
                Object selectedObj = getSelections().get(0);
                // Add to the selected declaring type
                if (selectedObj instanceof GroovySuggestionDeclaringType) {
                    dialogue = new InferencingContributionDialogue(getShell(), (GroovySuggestionDeclaringType) selectedObj,
                            getSelectedProject());
                } else if (selectedObj instanceof IGroovySuggestion) {
                    // adds to the declaring type of the selected suggestion
                    dialogue = new InferencingContributionDialogue(getShell(), (IGroovySuggestion) selectedObj,
                            getSelectedProject());
                }
            } else {
                // else no selection made, so allow the user to specify which
                // declaring type to add to through the suggestions dialogue
                dialogue = new InferencingContributionDialogue(getShell(), getSelectedProject());
            }

            if (dialogue != null && dialogue.open() == Window.OK) {
                SuggestionDescriptor descriptor = dialogue.getSuggestionChange();

                GroovySuggestionDeclaringType type = getDeclaringType(descriptor.getDeclaringTypeName());
                if (type != null) {
                    IGroovySuggestion suggestion = type.createSuggestion(descriptor);

                    // Refresh first before setting the check state of the new
                    // suggestion
                    refresh();

                    // Update the check state of the new element
                    setCheckState(suggestion);
                }
            }
        }
    }

    protected GroovySuggestionDeclaringType getDeclaringType(String declaringTypeName) {
        IProject project = getSelectedProject();
        if (project != null) {
            return InferencingSuggestionsManager.getInstance().getSuggestions(project).getDeclaringType(declaringTypeName);
        }
        return null;
    }

    protected void handleButtonSelection(ButtonTypes button) {
        if (button != null) {
            switch (button) {
                case ADD:
                    addSuggestion();
                    break;
                case EDIT:
                    editSuggestion();
                    break;
                case COLLAPSE_ALL:
                    collapseAll();
                    break;
                case DESELECT_ALL:
                    uncheckAll();
                    break;
                case REMOVE:
                    handleRemove();
                    break;
                case SELECT_ALL:
                    checkAll();
                    break;
                case EXPAND_ALL:
                    expandAll();
                    break;
            }
        }
    }

    protected void handleRemove() {
        List<Object> selections = getSelections();

        for (Object obj : selections) {
            if (obj instanceof GroovySuggestionDeclaringType) {
                InferencingSuggestionsManager.getInstance().getSuggestions(getSelectedProject())
                        .removeDeclaringType((GroovySuggestionDeclaringType) obj);
            } else if (obj instanceof IGroovySuggestion) {
                IGroovySuggestion suggestion = (IGroovySuggestion) obj;
                GroovySuggestionDeclaringType containingType = suggestion.getDeclaringType();
                containingType.removeSuggestion(suggestion);
                // Also remove the declaring type if no suggestions are left
                if (containingType.getSuggestions().isEmpty()) {
                    InferencingSuggestionsManager.getInstance().getSuggestions(getSelectedProject())
                            .removeDeclaringType(containingType);
                }
            }
        }
        refresh();
    }

    /**
     * Resets the viewer input with all the suggestions for the given project in
     * expanded form. Should only be called when changing projects or displaying
     * a list of suggestions for an initial selected project
     * 
     * @param project
     */
    protected void setViewerInput(IProject project) {
        if (isViewerDisposed() || project == null) {
            return;
        }

        Collection<GroovySuggestionDeclaringType> declaringTypes = InferencingSuggestionsManager.getInstance()
                .getSuggestions(project).getDeclaringTypes();
        if (declaringTypes == null) {
            return;
        }

        viewer.getTreeViewer().setInput(declaringTypes);
        refresh();

        // Set check state of each element after refresh
        for (GroovySuggestionDeclaringType declaringType : declaringTypes) {
            setCheckState(declaringType);
        }
    }

    protected void refresh() {
        viewer.getTreeViewer().refresh(true);
        expandAll();
    }

    protected boolean isViewerDisposed() {
        if (viewer == null || viewer.getTreeViewer() == null || viewer.getTreeViewer().getTree().isDisposed()) {
            return true;
        }
        return false;
    }

    protected static class ViewerContentProvider implements ITreePathContentProvider {
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Collection<?>) {
                List<Object> suggestedTypes = new ArrayList<Object>();
                Collection<?> topLevel = (Collection<?>) inputElement;
                for (Object possibleTypeSuggestion : topLevel) {
                    if (possibleTypeSuggestion instanceof GroovySuggestionDeclaringType) {
                        suggestedTypes.add(possibleTypeSuggestion);
                    }
                }
                return suggestedTypes.toArray();
            }
            return null;
        }

        public Object[] getChildren(TreePath path) {
            Object lastElement = path.getLastSegment();
            if (lastElement instanceof GroovySuggestionDeclaringType) {
                GroovySuggestionDeclaringType treeElement = (GroovySuggestionDeclaringType) lastElement;

                Set<IGroovySuggestion> properties = treeElement.getSuggestions();

                if (properties != null) {
                    return properties.toArray();
                }
            }
            return null;
        }

        public TreePath[] getParents(Object element) {
            return new TreePath[] {};
        }

        public boolean hasChildren(TreePath path) {
            return getChildren(path) != null;
        }

        public void dispose() {
            // nothing for now
        }

        public void inputChanged(Viewer viewer, Object e1, Object e2) {
            // nothing for now
        }
    }

    /**
     * Always returns a non-null selection list. May be empty if no selections
     * are present.
     * 
     * @return
     */
    protected List<Object> getSelections() {
        if (viewer.getTreeViewer().getSelection() instanceof IStructuredSelection) {
            return ((IStructuredSelection) viewer.getTreeViewer().getSelection()).toList();

        }
        return Collections.EMPTY_LIST;
    }

    protected static class ViewerLabelProvider extends ColumnLabelProvider {

        public void update(ViewerCell cell) {

            Object element = cell.getElement();
            int index = cell.getColumnIndex();

            cell.setText(getColumnText(element, index));
            cell.setImage(getColumnImage(element, index));
            cell.setFont(getFont(element));
        }

        public Image getColumnImage(Object element, int index) {
            return null;
        }

        public Font getFont(Object element) {
            return super.getFont(element);
        }

        public String getColumnText(Object element, int index) {

            ColumnTypes[] values = ColumnTypes.values();
            if (index < values.length) {
                ColumnTypes colType = values[index];

                String text = null;
                switch (colType) {
                    case SUGGESTIONS:
                        text = getDisplayString(element);
                        break;
                }
                return text;
            }

            return null;
        }
    }

    protected static String getDisplayString(Object element) {
        String text = null;
        if (element instanceof GroovySuggestionDeclaringType) {
            return ((GroovySuggestionDeclaringType) element).getName();
        } else if (element instanceof IGroovySuggestion) {
            ISuggestionLabel suggestionLabel = new SuggestionLabelFactory().getSuggestionLabel((IGroovySuggestion) element);
            if (suggestionLabel != null) {
                text = suggestionLabel.getLabel();
            }
        }
        return text;
    }

    protected void createOperationButtonArea(Composite parent) {

        Composite buttons = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().align(GridData.CENTER, GridData.BEGINNING).applyTo(buttons);

        GridLayoutFactory.fillDefaults().applyTo(buttons);

        ButtonTypes[] types = ButtonTypes.values();

        selectionButtons = new HashMap<GroovySuggestionsTable.ButtonTypes, Button>();
        for (ButtonTypes type : types) {
            Button button = createSelectionButton(buttons, type);
            if (button != null) {
                selectionButtons.put(type, button);
            }
        }

        handleSelectionButtonEnablement(getSelections());
    }

    protected Button createSelectionButton(Composite parent, final ButtonTypes type) {
        if (type == null) {
            return null;
        }

        Button button = new Button(parent, SWT.PUSH);
        button.setText(type.getLabel());
        button.setData(type);

        Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        int widthHint = 0;

        GridDataFactory.fillDefaults().hint(Math.max(widthHint, minSize.x), SWT.DEFAULT).applyTo(button);

        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {

                super.widgetSelected(e);
                Object item = e.getSource();
                if (item instanceof Button) {
                    Object widgetData = ((Button) item).getData();
                    if (widgetData instanceof ButtonTypes) {
                        handleButtonSelection((ButtonTypes) widgetData);
                    }
                }
            }
        });

        return button;
    }

    public static class SuggestionViewerSorter extends TreeViewerSorter {

        protected String getCompareString(TreeColumn column, Object rowItem) {
            ColumnTypes type = getColumnType(column);
            String text = null;
            if (type != null) {
                switch (type) {
                    case SUGGESTIONS:
                        text = getDisplayString(rowItem);
                        break;
                }
            }
            return text;
        }

        public int compare(Viewer viewer, Object e1, Object e2) {

            Tree tree = ((TreeViewer) viewer).getTree();

            TreeColumn sortColumn = tree.getSortColumn();

            ColumnTypes type = getColumnType(sortColumn);
            int sortDirection = 1;
            if (type != null) {
                switch (type) {
                    case SUGGESTIONS:

                        if (e1 instanceof GroovyPropertySuggestion) {
                            if (e2 instanceof GroovyPropertySuggestion) {
                                sortDirection = super.compare(viewer, e1, e2);
                            } else {
                                // Groovy Properties have higher sort order
                                return sortDirection == SWT.UP ? -1 : 1;
                            }
                        } else if (e1 instanceof GroovyMethodSuggestion) {
                            if (e2 instanceof GroovyMethodSuggestion) {
                                sortDirection = super.compare(viewer, e1, e2);
                            } else {
                                // Groovy Methods have lower sort order
                                return sortDirection == SWT.UP ? 1 : -1;
                            }
                        } else {
                            sortDirection = super.compare(viewer, e1, e2);
                        }
                        return sortDirection;
                }
            }

            return super.compare(viewer, e1, e2);
        }

        protected ColumnTypes getColumnType(TreeColumn column) {
            String columnName = column.getText();
            for (ColumnTypes type : ColumnTypes.values()) {
                if (type.equals(columnName)) {
                    return type;
                }
            }
            return null;
        }

    }

    public abstract static class TreeViewerSorter extends ViewerSorter {

        public int compare(Viewer viewer, Object e1, Object e2) {
            if (viewer instanceof TreeViewer) {
                Tree tree = ((TreeViewer) viewer).getTree();

                TreeColumn sortColumn = tree.getSortColumn();
                int sortDirection = tree.getSortDirection();
                if (sortColumn != null) {
                    String compareText1 = getCompareString(sortColumn, e1);
                    String compareText2 = getCompareString(sortColumn, e2);
                    if (compareText1 != null) {
                        if (compareText2 != null) {
                            return sortDirection == SWT.UP ? compareText1.compareToIgnoreCase(compareText2) : compareText2
                                    .compareToIgnoreCase(compareText1);
                        } else {
                            return sortDirection == SWT.UP ? -1 : 1;
                        }
                    } else if (compareText2 != null) {
                        return sortDirection == SWT.UP ? 1 : -1;
                    }
                }
            }

            return super.compare(viewer, e1, e2);
        }

        abstract protected String getCompareString(TreeColumn column, Object rowItem);

    }

}