
# Contributing to Pakku

## Quickstart

To build the `pakku.jar` run the `jvmJar` Gradle task (`gradlew jvmJar`).

To run tests, run the `jvmTest` Gradle task (`gradlew jvmTest`).

## Creating Commands & Parameters (Clikt)

### Introduction to commands

Pakku uses Clikt to handle the CLI. It is advised to [check out its docs](https://ajalt.github.io/clikt/).

Command classes should be called the same as the command itself.
If the command is a subcommand, name it using this pattern: `<parent-command><subcommand>`; example: `CfgPrj`.

Command parameters should be _suffixed_ with a special abbreviation of the parameter type. (`<parameter-name><abbrev>`)
Use: `Opt` for options, `Arg` for arguments, and `Flag` for flags (Boolean options).
If there can be multiple parameters use the plural, e.g. `Opts`, `Args`.

```kt
// Parameters should be always private.
// Defining parameters using a chain of function calls is preferred.

private val exampleOpt by option("--example") // Consult if a short version `-e` of the option is a good idea
    .help("...") // Consider discussing the help message
    .int()
    .optionalValue(2)
    .default(0)

private val exampleArg by argument("example")
    .help("...")

private val exampleFlag by option("-e").flag()
```

### Documenting commands

Documentation for commands can be generated automatically by running: `pakku --generate-docs`.

## Testing

When creating a test please extend the `PakkuTest` class. 
It will ensure every test is run separately in the `build/test/` directory,
and also automatically handle the working path of Pakku and teardown of any created files while running the tests.

For testing, we use the [Strikt](https://strikt.io/) assertion library because it is easy to use, read and debug.

Example of a test:

```kt
class ExampleTest : PakkuTest()
{
    private val testFileName = "test_file.txt" // Define test data at the top of the class

    override suspend fun `set-up`() // Override this function to implement set up
    { 
        /** 
         * Create test files or directories. (Will tear down automatically.)
         * `fun createTestFile(vararg path: String)`
         * `fun createTestDir(vararg path: String)`
         */
        createTestFile(testFileName)
    }

    // Use backticks for test function names.
    @Test
    fun `test if file exists`(): Unit = runBlocking {
        val file = testFile(testFileName) // Reference test files using `fun testFile(vararg path: String): Path`

        expectThat(file).get { exists() }.isTrue() // Test whether the file exists.
    }
}
```
