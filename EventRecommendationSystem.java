import java.io.*;
import java.util.*;

// Base Vertex class
abstract class Vertex implements Serializable {
    protected String id;
    protected List<Edge> edges;
    
    public Vertex(String id) {
        this.id = id;
        this.edges = new ArrayList<>();
    }
    
    public String getId() { return id; }
    public List<Edge> getEdges() { return edges; }
    public void addEdge(Edge edge) { if (!edges.contains(edge)) { edges.add(edge); } }
    public void removeEdge(Edge edge) { edges.remove(edge); }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vertex vertex = (Vertex) obj;
        return Objects.equals(id, vertex.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

// User class
class User extends Vertex {
    private final List<Event> attendedEvents;
    
    public User(String id) {
        super(id);
        this.attendedEvents = new ArrayList<>();
    }
    
    public List<Event> getAttendedEvents() { return attendedEvents; }
    public void addAttendedEvent(Event event) {
        if (!attendedEvents.contains(event)){
            attendedEvents.add(event);

            System.out.println("Attendance recorded.");
        } else {
            System.out.println(this.id + " has already attended this event.");
        }
    }
    public void removeAttendedEvent(Event event) { attendedEvents.remove(event); }
}

// Event class
class Event extends Vertex {
    private final List<String> categories;
    
    public Event(String id, List<String> categories) {
        super(id);
        this.categories = categories;
    }
    
    public List<String> getCategories() { return categories; }
}

// Category/Organizer class
class Category extends Vertex {
    public Category(String id) {
        super(id);
    }
}

// Edge class
class Edge implements Serializable {
    private final Vertex from;
    private final Vertex to;
    private final Double weight; // null if unweighted

    public Edge(Vertex from, Vertex to) {
        this(from, to, null);
    }

    public Edge(Vertex from, Vertex to, Double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public Vertex getFrom() { return from; }
    public Vertex getTo() { return to; }
    public Double getWeight() { return weight; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Edge edge = (Edge) obj;
        return Objects.equals(from, edge.from) && Objects.equals(to, edge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}

// Graph class to manage all vertices and edges
class EventGraph implements Serializable {
    private final Map<String, Vertex> vertices;
    
    public EventGraph() {
        this.vertices = new HashMap<>();
    }

    private String normalizeKey(String key){
        return key.toLowerCase().trim();
    }
    
    public void addVertex(Vertex vertex) {
        vertices.put(normalizeKey(vertex.getId()), vertex);
    }
    
    public Vertex getVertex(String id) {
        return vertices.get(normalizeKey(id));
    }
    
    public void removeVertex(String id) {
        Vertex vertex = vertices.remove(normalizeKey(id));
        if (vertex != null) {
            // Remove all edges connected to this vertex
            for (Vertex v : vertices.values()) {
                List<Edge> edgesToRemove = new ArrayList<>();
                for (Edge edge : v.getEdges()) {
                    if (edge.getTo().equals(vertex) || edge.getFrom().equals(vertex)) {
                        edgesToRemove.add(edge);
                    }
                }
                v.getEdges().removeAll(edgesToRemove);
            }
        }
    }
    

    // Add unweighted edge
    public void addEdge(Vertex from, Vertex to) {
        Edge edge = new Edge(from, to);
        from.addEdge(edge);
    }

    // Add weighted edge
    public void addEdge(Vertex from, Vertex to, Double weight) {
        Edge edge = new Edge(from, to, weight);
        from.addEdge(edge);
    }
    
    public void removeEdge(Vertex from, Vertex to) {
        Edge edgeToRemove = null;
        for (Edge edge : from.getEdges()) {
            if (edge.getTo().equals(to)) {
                edgeToRemove = edge;
                break;
            }
        }
        if (edgeToRemove != null) {
            from.removeEdge(edgeToRemove);
        }
    }
    
    public Map<String, Vertex> getVertices() {
        return vertices;
    }

    public boolean containsVertex(String id) {
        return vertices.containsKey(normalizeKey(id));
    }
    
    // BFS-based event recommendation
    public List<Event> recommendEvents(User user, int maxDepth) {
        List<Event> recommendations = new ArrayList<>();
        Set<Vertex> visited = new HashSet<>();
        Queue<Vertex> queue = new LinkedList<>();
        Map<Vertex, Integer> depths = new HashMap<>();

        // Get user's preferred category based on attended events
        Set<String> preferredCategories = getUserPreferredCategories(user);
        
        if (preferredCategories.isEmpty()) {
            // Prompt user to select preferred categories
            return recommendations; // No preferences, return empty list
        }
        
        // Start from user
        queue.add(user);
        visited.add(user);
        depths.put(user, 0);
        
        while (!queue.isEmpty()) {
            Vertex current = queue.poll();
            int currentDepth = depths.get(current);
            
            if (currentDepth >= maxDepth) {
                continue;
            }
            
            for (Edge edge : current.getEdges()) {
                Vertex neighbor = edge.getTo();
                
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    depths.put(neighbor, currentDepth + 1);
                    queue.add(neighbor);
                    
                    // Check if this is an event in the preferred category
                    if (neighbor instanceof Event event) {
                        boolean matchesPreferred = false;
                        for (String category : event.getCategories()) {
                            if (preferredCategories.contains(category)) {
                                matchesPreferred = true;
                                break;
                            }
                        }
                        if (matchesPreferred && 
                            !user.getAttendedEvents().contains(event)) {
                            recommendations.add(event);
                        }
                    }
                }
            }
        }
        
        return recommendations;
    }

    private Set<String> getUserPreferredCategories(User user) {
        Set<String> categories = new HashSet<>();
        
        for (Event event : user.getAttendedEvents()) {
            for (String category : event.getCategories()) {
                categories.add(category);
            }
        }

        return categories;
    } 
    
    // Save graph to file
    public void saveToFile(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Error saving graph: " + e.getMessage());
        }
    }
    
    // Load graph from file
    public static EventGraph loadFromFile(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (EventGraph) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading graph: " + e.getMessage());
            System.out.println("Creating a new graph.");
            return new EventGraph();
        }
    }
}

// Main application class
public class EventRecommendationSystem {
    private final EventGraph graph;
    private final String dataFile = "event_graph.dat";
    
    public EventRecommendationSystem() {
        // Try to load existing graph, or create a new one
        graph = EventGraph.loadFromFile(dataFile);

        if (graph.getVertices().isEmpty()) {
            initializeDefaultData();
        }
    }

    // Initialize with some default data
    private void initializeDefaultData() {
        String[] categories = {"concert", "comedy", "charity", "theatre", "workshops"};
        for (String category : categories) {
            addCategory(category);
        }

        addUser("A");
        addUser("B");
        addUser("C");

        addEvent("ComedyClash", Arrays.asList("comedy", "theatre"));
        addEvent("KIDULTING! Comedy Special", Arrays.asList("comedy", "theatre"));
        addEvent("Mads Comedy Night", Arrays.asList("comedy", "theatre"));
        addEvent("FantasticDuoConcert", Arrays.asList("concert"));
        addEvent("Lovely Day Charity Concert", Arrays.asList("charity", "concert"));
        addEvent("VarietyCharityConcert", Arrays.asList("charity", "concert", "theatre"));
        addEvent("PythonWorkshop", Arrays.asList("workshops"));
        addEvent("AI Bootcamp", Arrays.asList("workshops"));

        recordAttendance("A", "ComedyClash");
        recordAttendance("B", "FantasticDuoConcert");
        recordAttendance("B", "VarietyCharityConcert");
        recordAttendance("C", "PythonWorkshop");
        recordAttendance("C", "AI Bootcamp");
        recordAttendance("C", "Mads Comedy Night");

        graph.saveToFile(dataFile);
    }
    
    // Menu operations
    public void addUser(String userId) {
        if (!graph.containsVertex(userId)) {
            graph.addVertex(new User(userId));
            graph.saveToFile(dataFile);

            System.out.println("User added.");
        } else {
            System.out.println("User already exists.");
        }
    }
    
    public void removeUser(String userId) {
        if (graph.containsVertex(userId)) {
            graph.removeVertex(userId);
            graph.saveToFile(dataFile);

            System.out.println("User removed.");
        } else {
            System.out.println("User does not exist.");
        }
    }
    
    // Add event with multiple categories
    public void addEvent(String eventId, List<String> categories) {
        if (!graph.containsVertex(eventId)) {
            Event event = new Event(eventId, categories);
            graph.addVertex(event);
            for (String category : categories) {
                Vertex categoryVertex = graph.getVertex(category);
                if (categoryVertex == null) {
                    addCategory(category);
                    categoryVertex = graph.getVertex(category);
                }
                graph.addEdge(event, categoryVertex);
                graph.addEdge(categoryVertex, event); // Bidirectional
            }
            graph.saveToFile(dataFile);
            System.out.println("Event added.");
        } else {
            System.out.println("Event already exists.");
        }
    }

    // Remove an event
    public void removeEvent(String eventId) {
        if (graph.containsVertex(eventId) && graph.getVertex(eventId) instanceof Event) {
            graph.removeVertex(eventId);
            graph.saveToFile(dataFile);
            System.out.println("Event removed.");
        } else {
            System.out.println("Event does not exist.");
        }
    }
    
    public void addCategory(String categoryId) {
        if (!graph.containsVertex(categoryId)) {
            graph.addVertex(new Category(categoryId));
            graph.saveToFile(dataFile);

            System.out.println("Category added.");
        } else {
            System.out.println("Category already exists.");
        }
    }

    // Remove a category
    public void removeCategory(String categoryId) {
        if (graph.containsVertex(categoryId) && graph.getVertex(categoryId) instanceof Category) {
            graph.removeVertex(categoryId);
            graph.saveToFile(dataFile);
            System.out.println("Category removed.");
        } else {
            System.out.println("Category does not exist.");
        }
    }
    
    public void recordAttendance(String userId, String eventId) {
        Vertex userVertex = graph.getVertex(userId);
        Vertex eventVertex = graph.getVertex(eventId);

        if (userVertex instanceof User && eventVertex instanceof Event) {
            User user = (User) userVertex;
            Event event = (Event) eventVertex;

            // Prompt for rating
            Scanner scanner = new Scanner(System.in);
            double rating = -1;
            while (rating < 0 || rating > 5) {
                System.out.print("Enter your rating for the event (0-5): ");
                String input = scanner.nextLine();
                try {
                    rating = Double.parseDouble(input);
                    if (rating < 0 || rating > 5) {
                        System.out.println("Invalid rating. Please enter a value between 0 and 5.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number.");
                }
            }

            // Add weighted edge from user to event
            graph.addEdge(user, event, rating);

            // Record attendance in user's history
            user.addAttendedEvent(event);

            graph.saveToFile(dataFile);
        } else {
            if (!(userVertex instanceof User)) {
                System.out.println("User does not exist.");
            } else {
                System.out.println("Event does not exist.");
            }
        }
    }
    // Add friendship (bidirectional) between users
    public void addFriendship(String userId1, String userId2) {
        Vertex v1 = graph.getVertex(userId1);
        Vertex v2 = graph.getVertex(userId2);
        if (v1 instanceof User && v2 instanceof User) {
            // Add bidirectional edges (unweighted)
            graph.addEdge(v1, v2);
            graph.addEdge(v2, v1);
            graph.saveToFile(dataFile);
            System.out.println("Friendship added between " + userId1 + " and " + userId2 + ".");
        } else {
            System.out.println("Both users must exist to add friendship.");
        }
    }
    
    public List<Event> recommendEvents(String userId, int maxDepth, Scanner scanner) {
        Vertex userVertex = graph.getVertex(userId);
        if (userVertex instanceof User user) {
            // If user has never attended an event, prompt for preferred categories
            if (user.getAttendedEvents().isEmpty()) {
                List<String> preferredCategories = selectCategories(scanner, "You have not attended any events yet. Select your preferred categories for recommendations:");
                List<Event> recommendations = new ArrayList<>();
                for (Vertex v : graph.getVertices().values()) {
                    if (v instanceof Event event) {
                        for (String cat : event.getCategories()) {
                            if (preferredCategories.contains(cat)) {
                                recommendations.add(event);
                                break;
                            }
                        }
                    }
                }
                return recommendations;
            } else {
                return graph.recommendEvents(user, maxDepth);
            }
        }
        return new ArrayList<>();
    }
    
    // public EventGraph getGraph() {
    //     return graph;
    // }
    
    // For your teammate to visualize the graph
    public Map<String, List<String>> getAdjacencyList() {
        Map<String, List<String>> adjacencyList = new HashMap<>();
        
        for (Vertex vertex : graph.getVertices().values()) {
            List<String> connections = new ArrayList<>();
            for (Edge edge : vertex.getEdges()) {
                connections.add(edge.getTo().getId());
            }
            adjacencyList.put(vertex.getId(), connections);
        }
        
        return adjacencyList;
    }

    // Get name input
    private String getNameInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // List all users and let user select one by number
    private String selectUser(Scanner scanner, String prompt) {
        List<String> users = new ArrayList<>();
        for (Vertex v : graph.getVertices().values()) {
            if (v instanceof User) users.add(v.getId());
        }
        if (users.isEmpty()) {
            System.out.println("No users available.");
            return null;
        }
        System.out.println(prompt);
        for (int i = 0; i < users.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, users.get(i));
        }
        int choice = getNumberInput(scanner, 1, users.size());
        return users.get(choice - 1);
    }

    // List all categories and let user select one by number
    public String selectCategory(Scanner scanner, String prompt) {
        List<String> categories = new ArrayList<>();
        for (Vertex v : graph.getVertices().values()) {
            if (v instanceof Category) categories.add(v.getId());
        }
        if (categories.isEmpty()) {
            System.out.println("No categories available.");
            return null;
        }
        System.out.println(prompt);
        for (int i = 0; i < categories.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, categories.get(i));
        }
        int choice = getNumberInput(scanner, 1, categories.size());
        return categories.get(choice - 1);
    }

    // List all categories and let user select one or more by numbers (comma-separated)
    public List<String> selectCategories(Scanner scanner, String prompt) {
        List<String> categories = new ArrayList<>();
        for (Vertex v : graph.getVertices().values()) {
            if (v instanceof Category) categories.add(v.getId());
        }
        if (categories.isEmpty()) {
            System.out.println("No categories available.");
            return Collections.emptyList();
        }
        System.out.println(prompt);
        for (int i = 0; i < categories.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, categories.get(i));
        }
        List<String> selected = new ArrayList<>();
        while (selected.isEmpty()) {
            System.out.printf("Enter number(s) separated by comma (e.g. 1,3): ");
            String input = scanner.nextLine();
            String[] parts = input.split(",");
            for (String part : parts) {
                try {
                    int idx = Integer.parseInt(part.trim());
                    if (idx >= 1 && idx <= categories.size()) {
                        String cat = categories.get(idx - 1);
                        if (!selected.contains(cat)) selected.add(cat);
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (selected.isEmpty()) System.out.println("Invalid input. Please select at least one category.");
        }
        return selected;
    }

    // List all categories and let user select one by number
    // private String selectCategory(Scanner scanner, String prompt) {
    //     List<String> categories = new ArrayList<>();
    //     for (Vertex v : graph.getVertices().values()) {
    //         if (v instanceof Category) categories.add(v.getId());
    //     }
    //     if (categories.isEmpty()) {
    //         System.out.println("No categories available.");
    //         return null;
    //     }
    //     System.out.println(prompt);
    //     for (int i = 0; i < categories.size(); i++) {
    //         System.out.printf("%d. %s\n", i + 1, categories.get(i));
    //     }
    //     int choice = getNumberInput(scanner, 1, categories.size());
    //     return categories.get(choice - 1);
    // }

    // List all events and let user select one by number
    private String selectEvent(Scanner scanner, String prompt) {
        List<String> events = new ArrayList<>();
        for (Vertex v : graph.getVertices().values()) {
            if (v instanceof Event) events.add(v.getId());
        }
        if (events.isEmpty()) {
            System.out.println("No events available.");
            return null;
        }
        System.out.println(prompt);
        for (int i = 0; i < events.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, events.get(i));
        }
        int choice = getNumberInput(scanner, 1, events.size());
        return events.get(choice - 1);
    }

    // Helper to get a number input in a range
    private int getNumberInput(Scanner scanner, int min, int max) {
        int choice = -1;
        while (true) {
            System.out.printf("Enter number (%d-%d): ", min, max);
            String input = scanner.nextLine();
            try {
                choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) break;
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid input. Try again.");
        }
        return choice;
    }

    // Pause for user input before clearing screen
    private static void pauseForUser(Scanner scanner) {
        System.out.print("Press Enter to return...");
        scanner.nextLine();
    }
    
    // Clear the console screen (works for Windows, Mac, Linux)
    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback: print newlines
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }

    // Example usage
    public static void main(String[] args) {
        EventRecommendationSystem system = new EventRecommendationSystem();

        // Menu for graph operations
        Scanner scanner = new Scanner(System.in);
        String command;

        do {
            System.out.println("\nMenu:");
            System.out.println("1. Add User");
            System.out.println("2. Remove User");
            System.out.println("3. Add Event");
            System.out.println("4. Add Category");
            System.out.println("5. Record Attendance");
            System.out.println("6. Recommend Events");
            System.out.println("7. Remove Category");
            System.out.println("8. Remove Event");
            System.out.println("9. View Graph");
            System.out.println("10. Exit");
            System.out.println("11. Add Friendship (between users)");
            System.out.print("Enter choice: ");
            command = scanner.nextLine();

            switch (command) {
                case "11" -> {
                    // Add friendship
                    String user1 = system.selectUser(scanner, "Select first user for friendship:");
                    String user2 = system.selectUser(scanner, "Select second user for friendship:");
                    if (user1 != null && user2 != null && !user1.equals(user2)) {
                        system.addFriendship(user1, user2);
                    } else {
                        System.out.println("Invalid users selected or same user chosen.");
                    }
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "1" -> {
                    String username = system.getNameInput(scanner, "Enter new User Name: ");
                    system.addUser(username);
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "2" -> {
                    String username = system.selectUser(scanner, "Select User to remove:");
                    if (username != null) system.removeUser(username);
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "3" -> {
                    String eventName = system.getNameInput(scanner, "Enter Event Name: ");
                    List<String> categories = system.selectCategories(scanner, "Select one or more Event Categories:");
                    if (!categories.isEmpty()) system.addEvent(eventName, categories);
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "4" -> {
                    String categoryName = system.getNameInput(scanner, "Enter new Category Name: ");
                    system.addCategory(categoryName);
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "5" -> {
                    String username = system.selectUser(scanner, "Select User:");
                    String eventName = system.selectEvent(scanner, "Select Event:");
                    if (username != null && eventName != null) system.recordAttendance(username, eventName);
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "6" -> {
                    String username = system.selectUser(scanner, "Select User for recommendations:");
                    clearScreen();
                    if (username != null) {
                        List<Event> recommendations = system.recommendEvents(username, 6, scanner);
                        if (recommendations.isEmpty()) {
                            System.out.println("No recommendations available.");
                        } else {
                            System.out.println("Recommended Events:");
                            for (Event event : recommendations) {
                                System.out.println("- " + event.getId());
                            }
                        }
                    }
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "7" -> {
                    String category = system.selectCategory(scanner, "Select Category to remove:");
                    if (category != null) system.removeCategory(category);
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "8" -> {
                    String event = system.selectEvent(scanner, "Select Event to remove:");
                    if (event != null) system.removeEvent(event);
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "9" -> {
                    clearScreen();
                    Map<String, List<String>> adjList = system.getAdjacencyList();
                    System.out.println("Adjacency List:");
                    for (Map.Entry<String, List<String>> entry : adjList.entrySet()) {
                        System.out.println(entry.getKey() + " -> " + entry.getValue());
                    }
                    pauseForUser(scanner);
                    clearScreen();
                }
                case "10" -> System.out.println("Exiting...");
                default -> {
                    System.out.println("Invalid command. Please try again.");
                    pauseForUser(scanner);
                    clearScreen();
                }
            }
        } while (!command.equals("10"));

        scanner.close();
    }
}
