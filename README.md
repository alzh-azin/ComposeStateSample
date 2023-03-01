# State and Jetpack Compose

## State and composition

Compose is declarative and as such the only way to update it is by calling the same composable with new arguments. These arguments are representations of the UI state. Any time a state is updated a *recomposition* takes place.

```kotlin
@Composable
private fun HelloContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Hello!",
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Name") }
        )
    }
}
```

If you run this, you'll see that nothing happens. That's because the `TextField` doesn't update itself—it updates when its `value` parameter changes. This is due to how composition and recomposition work in Compose.

> **Key Term:** **Composition:** a description of the UI built by Jetpack Compose when it executes composables.
> 
> **Initial composition:** creation of a Composition by running composables the first time.
> 
> **Recomposition:** re-running composables to update the Composition when data changes.

## State in composables

Composable functions can use the [`remember`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#remember(kotlin.Function0)) API to store an object in memory. A value computed by `remember` is stored in the Composition during initial composition, and the stored value is returned during recomposition. `remember` can be used to store both mutable and immutable objects.

> **Note:** `remember` stores objects in the Composition, and forgets the object when the composable that called `remember` is removed from the Composition.

[`mutableStateOf`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#mutableStateOf(kotlin.Any,androidx.compose.runtime.SnapshotMutationPolicy)) creates an observable [`MutableState<T>`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/MutableState), which is an observable type integrated with the compose runtime.

Any changes to `value` schedules recomposition of any composable functions that read `value`. In the case of `ExpandingCard`, whenever `expanded` changes, it causes `ExpandingCard` to be recomposed.

While `remember` helps you retain state across recompositions, the state is not retained across configuration changes. For this, you must use `rememberSaveable`. `rememberSaveable` automatically saves any value that can be saved in a `Bundle`. For other values, you can pass in a custom saver object.

### Stateful versus stateless

A composable that uses `remember` to store an object creates internal state, making the composable *stateful*. `HelloContent` is an example of a stateful composable because it holds and modifies its `name` state internally. This can be useful in situations where a caller doesn't need to control the state and can use it without having to manage the state themselves. However, composables with internal state tend to be less reusable and harder to test.

A *stateless* composable is a composable that doesn't hold any state. An easy way to achieve stateless is by using [state hoisting](https://developer.android.com/jetpack/compose/state#state-hoisting).

As you develop reusable composables, you often want to expose both a stateful and a stateless version of the same composable. The stateful version is convenient for callers that don't care about the state, and the stateless version is necessary for callers that need to control or hoist the state.

## State hoisting

State hoisting in Compose is a pattern of moving state to a composable's caller to make a composable stateless. The general pattern for state hoisting in Jetpack Compose is to replace the state variable with two parameters:

- **`value: T`:** the current value to display
- **`onValueChange: (T) -> Unit`:** an event that requests the value to change, where `T` is the proposed new value

State that is hoisted this way has some important properties:

- **Single source of truth:** By moving state instead of duplicating it, we're ensuring there's only one source of truth. This helps avoid bugs.
  
- **Encapsulated:** Only stateful composables can modify their state. It's completely internal.
  
- **Shareable:** Hoisted state can be shared with multiple composables. If you wanted to read `name` in a different composable, hoisting would allow you to do that.
  
- **Interceptable:** callers to the stateless composables can decide to ignore or modify events before changing the state.
  
- **Decoupled:** the state for the stateless `ExpandingCard` may be stored anywhere. For example, it's now possible to move `name` into a `ViewModel`.
  

By hoisting the state out of `HelloContent`, it's easier to reason about the composable, reuse it in different situations, and test. `HelloContent` is decoupled from how its state is stored. Decoupling means that if you modify or replace `HelloScreen`, you don't have to change how `HelloContent` is implemented.

![stateeventpng](https://github.com/alzh-azin/ComposeStateSample/blob/master/readmeResources/state-event.png?raw=true "state-event")

The pattern where the state goes down, and events go up is called a *unidirectional data flow*. In this case, the state goes down from `HelloScreen` to `HelloContent` and events go up from `HelloContent` to `HelloScreen`. By following unidirectional data flow, you can decouple composables that display state in the UI from the parts of your app that store and change state.

> **Key Point:** When hoisting state, there are three rules to help you figure out where state should go:
> 
> 1. State should be hoisted to at *least* the **lowest common parent** of all composables that use the state (read).
> 2. State should be hoisted to at *least* the **highest level it may be changed** (write).
> 3. If **two states change in response to the same events** they should be **hoisted together.**
> 
> You can hoist state higher than these rules require, but underhoisting state makes it difficult or impossible to follow unidirectional data flow.

## Restoring state in Compose

### Ways to store state

All data types that are added to the `Bundle` are saved automatically. If you want to save something that cannot be added to the `Bundle`, there are several options.

#### Parcelize

The simplest solution is to add the [`@Parcelize`](https://github.com/Kotlin/KEEP/blob/master/proposals/extensions/android-parcelable.md) annotation to the object. The object becomes parcelable, and can be bundled. For example, this code makes a parcelable `City` data type and saves it to the state.

```kotlin
@Parcelize
data class City(val name: String, val country: String) : Parcelable

@Composable
fun CityScreen() {
    var selectedCity = rememberSaveable {
        mutableStateOf(City("Madrid", "Spain"))
    }
}
```

#### MapSaver

If for some reason `@Parcelize` is not suitable, you can use `mapSaver` to define your own rule for converting an object into a set of values that the system can save to the `Bundle`.

```kotlin
data class City(val name: String, val country: String)

val CitySaver = run {
    val nameKey = "Name"
    val countryKey = "Country"
    mapSaver(
        save = { mapOf(nameKey to it.name, countryKey to it.country) },
        restore = { City(it[nameKey] as String, it[countryKey] as String) }
    )
}

@Composable
fun CityScreen() {
    var selectedCity = rememberSaveable(stateSaver = CitySaver) {
        mutableStateOf(City("Madrid", "Spain"))
    }
}
```

#### ListSaver

To avoid needing to define the keys for the map, you can also use `listSaver` and use its indices as keys:

```kotlin
data class City(val name: String, val country: String)

val CitySaver = listSaver<City, Any>(
    save = { listOf(it.name, it.country) },
    restore = { City(it[0] as String, it[1] as String) }
)

@Composable
fun CityScreen() {
    var selectedCity = rememberSaveable(stateSaver = CitySaver) {
        mutableStateOf(City("Madrid", "Spain"))
    }
}
```

## State holders in Compose

Simple state hoisting can be managed in the composable functions itself. However, if the amount of state to keep track of increases, or the logic to perform in composable functions arises, it's a good practice to delegate the logic and state responsibilities to other classes: **state holders**.

## Retrigger remember calculations when keys change

The [`remember`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#remember(kotlin.Any,kotlin.Any,kotlin.Any,kotlin.Function0)) API is frequently used together with [`MutableState`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/MutableState):

```kotlin
var name by remember { mutableStateOf("") }
```

Here, using the `remember` function makes the `MutableState` value survive recompositions.

In general, `remember` takes a `calculation` lambda parameter. When `remember` is first run, it invokes the `calculation` lambda and stores its result. During recomposition, `remember` returns the value that was last stored.

Apart from caching state, you can also use `remember` to store any object or result of an operation in the Composition that is expensive to initialize or calculate. You might not want to repeat this calculation in every recomposition. An example is creating this [`ShaderBrush`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/ShaderBrush) object, which is an expensive operation:

```kotlin
val brush = remember {
    ShaderBrush(
        BitmapShader(
            ImageBitmap.imageResource(res, avatarRes).asAndroidBitmap(),
            Shader.TileMode.REPEAT,
            Shader.TileMode.REPEAT
        )
    )
}
```

`remember` stores the value until it leaves the Composition. However, there is a way to invalidate the cached value. The `remember` API also takes a `key` or `keys` parameter. *If any of these keys change, the next time the function recomposes*, `remember` *invalidates the cache and executes the calculation lambda block again*. This mechanism gives you control over the lifetime of an object in the Composition. The calculation remains valid until the inputs change, instead of until the remembered value leaves the Composition.

The following examples show how this mechanism works.

In this snippet, a [`ShaderBrush`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/ShaderBrush) is created and used as the background paint of a `Box` composable. `remember` stores the [`ShaderBrush`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/ShaderBrush) instance because it is expensive to recreate, as explained earlier. `remember` takes `avatarRes` as the `key1` parameter, which is the selected background image. If `avatarRes` changes, the brush recomposes with the new image, and reapplies to the `Box`. This can occur when the user selects another image to be the background from a picker.

```kotlin
@Composable
private fun BackgroundBanner(
    @DrawableRes avatarRes: Int,
    modifier: Modifier = Modifier,
    res: Resources = LocalContext.current.resources
) {
    val brush = remember(key1 = avatarRes) {
        ShaderBrush(
            BitmapShader(
                ImageBitmap.imageResource(res, avatarRes).asAndroidBitmap(),
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT
            )
        )
    }

    Box(
        modifier = modifier.background(brush)
    ) {
        /* ... */
    }
}
```
