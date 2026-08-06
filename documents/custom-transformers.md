# Custom filter transformers

Applications can customize documents for gateway filters by implementing `OpenApiFilterTransformer`.

```java
@Component
public class ExampleTransformer implements OpenApiFilterTransformer {
    @Override
    public String getFilterName() {
        return "ExampleFilter";
    }

    @Override
    public void transform(ObjectNode document, Map<String, String> arguments) {
        // Update the document using the filter arguments.
    }
}
```

The transformer is invoked when the route contains a filter with the matching name. Return `false` from `isEnabled()` when the transformer should be disabled.
