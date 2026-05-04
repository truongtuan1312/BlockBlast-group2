public class UndoManager {
    private GameStateMemento savedState;

    public void save(GameStateMemento memento) {
        this.savedState = memento;
    }

    public GameStateMemento restore() {
        GameStateMemento state = savedState;
        savedState = null;
        return state;
    }

    public boolean canUndo() {
        return savedState != null;
    }
}
