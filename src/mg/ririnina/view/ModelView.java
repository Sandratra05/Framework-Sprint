package mg.ririnina.view;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    public String view;
    public Map<String, Object> items = new HashMap<>();

    public ModelView() {
    }

    public ModelView(String view, Map<String, Object> items) {
        this.setView(view);
        this.setItems(items);
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getItems() {
        return items;
    }

    public void setItems(Map<String, Object> items) {
        this.items = items;
    }

    public void addAttribute(String name, Object value) {
        this.items.put(name, value);
    }    
}
