import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameObjectPool<T extends Drawable> {
    private final List<T> objects = new ArrayList<>();

    public void add(T obj) {
        objects.add(obj);
    }

    public void updateAll() {
        objects.forEach(T::update);
    }

    public void drawAll(java.awt.Graphics2D g2) {
        objects.forEach(obj -> obj.draw(g2));
    }

    public void removeDeadObjects() {
        Iterator<T> it = objects.iterator();
        while (it.hasNext()) {
            if (it.next().isDead()) it.remove();
        }
    }

    public void clear() {
        objects.clear();
    }

    public int size() {
        return objects.size();
    }

    public List<T> getAll() {
        return objects;
    }
}
