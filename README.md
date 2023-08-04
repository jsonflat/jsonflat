# json flattener
Java module to flatten json-line logs with complex nested elements.
Also it contains CLI utility for fast transformation json files.
 
## Example

**test.json**
```json
{
  "store": {
    "book": [
      {
        "category": "reference",
        "author": "Nigel Rees",
        "title": "Sayings of the Century",
        "price": 8.95
      },
      {
        "category": "fiction",
        "author": "Evelyn Waugh",
        "title": "Sword of Honour",
        "price": 12.99
      },
      {
        "category": "fiction",
        "author": "Herman Melville",
        "title": "Moby Dick",
        "isbn": "0-553-21311-3",
        "price": 8.99
      },
      {
        "category": "fiction",
        "author": "J. R. R. Tolkien",
        "title": "The Lord of the Rings",
        "isbn": "0-395-19395-8",
        "price": 22.99
      }
    ],
    "bicycle": {
      "color": "red",
      "price": 19.95
    }
  },
  "expensive": 10
}
```
**Result**
```json lines
{"store_book_category":"reference","store_book_author":"Nigel Rees","store_book_title":"Sayings of the Century","store_book_price":8.95,"store_bicycle_color":"red","store_bicycle_price":19.95,"expensive":10}
{"store_book_category":"fiction","store_book_author":"Evelyn Waugh","store_book_title":"Sword of Honour","store_book_price":12.99,"store_bicycle_color":"red","store_bicycle_price":19.95,"expensive":10}
{"store_book_category":"fiction","store_book_author":"Herman Melville","store_book_title":"Moby Dick","store_book_price":8.99,"store_book_isbn":"0-553-21311-3","store_bicycle_color":"red","store_bicycle_price":19.95,"expensive":10}
{"store_book_category":"fiction","store_book_author":"J. R. R. Tolkien","store_book_title":"The Lord of the Rings","store_book_price":22.99,"store_book_isbn":"0-395-19395-8","store_bicycle_color":"red","store_bicycle_price":19.95,"expensive":10}
```

## For java library usage
For embedding in a program use class `io.github.jsonflat.Transformer`.
Method `transform` takes json-документ as a string, and returns a list of "flat" JSON strings. To work it needs a scheme of processing of the document.

## Scheme
Scheme defines what a data from the document needs to be added to the result. Also it also allows to set parameters of node processing.
There are several ways to define scheme. The most simple is to define scheme by list of paths names you need in result. In this case, nested paths will need to be described by compound names through the underscore character '_'. Also when describing names, wildcard is supported by using the characters '*' and '?''. 

When parsing nested arrays, the order of the elements is preserved.
For example, if you set the  schema for the document above:
* store_book_title
* store_book_price
* store_bicycle*

then the result of the parser's work will be documents: 
```json lines
{"store_book_title":"Sayings of the Century","store_book_price":8.95,"store_bicycle_color":"red","store_bicycle_price":19.95}
{"store_book_title":"Sword of Honour","store_book_price":12.99,"store_bicycle_color":"red","store_bicycle_price":19.95} 
{"store_book_title":"Moby Dick","store_book_price":8.99,"store_bicycle_color":"red","store_bicycle_price":19.95}
{"store_book_title":"The Lord of the Rings","store_book_price":22.99,"store_bicycle_color":"red","store_bicycle_price":19.95}
```
Java code to apply the transformation:
```java
        Schema schema = AutoSchemaFactory.builder()
                .columnStringFilters(Arrays.asList("store_book_title", "store_book_price", "store_bicycle*"))
                .build()
                .generate(jsonText);
        Transformer transformer = new Transformer(schema);
        List<String> result = transformer.transform(jsonText);
```
### Automatic schema generation
`io.github.jsonflat.schema.AutoSchemaFactory` builds a document processing schema automatically based on the incoming document.
In addition `io.github.jsonflat.schema.AutoSchemaFactory` allows you to define:
* [JsonPath](https://github.com/json-path/JsonPath) to filter json strings that should be processed (filtering is disabled by default)
* separator used to form field names (default '_')
* grouping policy for arrays of primitive types (by default ARRAY)
* policy grouping arrays of structures (by default NO_GROUP)

The following policies for processing nested json arrays have been implemented:
* CONCAT – concatenation of values into a string separated by commas ',';
* ARRAY - leave as an array as a result
* COLUMNS - each element of the array will be converted to a separate field in the resulting json with the name of the array index
* NO_GROUP - do not group array elements, each element will correspond to a separate document in the resulting array

For example, if you set the policy for processing arrays of structures as `COLUMNS` then the result of the parser's work will be:
```json lines
{"store_book_0_title":"Sayings of the Century","store_book_0_price":8.95,"store_book_1_title":"Sword of Honour","store_book_1_price":12.99,"store_book_2_title":"Moby Dick","store_book_2_price":8.99,"store_book_3_title":"The Lord of the Rings","store_book_3_price":22.99,"store_bicycle_color":"red","store_bicycle_price":19.95}
```
Other examples of setting schemas via an external JSON file can be found in the test classes: `AutoSchemaTest` and `AutoSchemaEveryLineTest`


### Schema definition via external JSON file
`io.github.jsonflat.schema.JsonSchemaFactory` specifies a schema for parsing a document based on an external json file. In the description of the scheme, it will be necessary to describe the nesting of the structures of the source json file and it is possible to describe the transformation rules for each node.
The scheme defines:
- `name` – string, schema name, does not affect document parsing
- `version` – string, schema version, does not affect document parsing
- `filter` - an object for filtering json strings that should be processed (filtering is disabled by default)
  - `path` - [JsonPath](https://github.com/json-path/JsonPath) based on which the incoming document will or will not be processed
  - `class` - filtering class, must inherit the `io.github.jsonflat.schema.filter.Filter` interface
    - `io.github.jsonflat.schema.filter.Exist` - if the specified JsonPath exists, then the document will be processed
    - `io.github.jsonflat.schema.filter.NotExist` - if the specified JsonPath exists, then the document will be skipped
- `columnResults` - array of objects, list of columns. The columns have a hierarchical structure. Only leaf columns will be displayed in the final table
  - `name` - string column name, used to name fields in the resulting document
  - `path` - JsonPath string to get column value from document. The path is always relative to the parent node. Optional field, if absent, then the value from the name field will be used as the path. Important note: if the element contains an array of objects, then it must be specified explicitly via JsonPath (for example, selecting all objects of the '[*]' array)
  - `fullname` - boolean, defines the column naming rule in the final table.
    - `false` - the column name will be formed by concatenating the names of all parent columnResults, in natural order (used by default)
    - `true`, - use `name` value as name of column.
  - `skipJsonIfEmpty` - boolean, skips document conversion if there is no such field in the document.
    - `true` – skipped document will not be displayed in the final table.
    - `false` - if the field is missing, the document will be present in the final table without the specified field (used by default)
  - `skipRowIfEmpty` - boolean, skip this row of the final table, if after field conversion its value becomes null. Essentially guarantees that all rows in the result will contain the specified column.
    - `true` – if the field in the final table is null, then do not display the row in the final table
    - `false` - if the field in the final table is null, then output the row to the final table without this field (used by default)
  - `group` - enum, policy for combining field values if there are several of them (see array processing policies). The default is NO_GROUP.
  - `converter` - an object, a rule for converting json values when writing to a table, if required. Optional field, by default the column type in the final table will match the value type in json.
    - `class` - a string, the class that performs the conversion must inherit the `io.github.jsonflat.schema.converter.Converter` interface
      - `io.github.jsonflat.schema.converter.ToDatetime` - convert to yyyy-MM-dd'T'HH:mm:ss format from UNIX milliseconds format
        - `pattern` - date pattern
      - `io.github.jsonflat.schema.converter.ToLong` - conversion to Long number
      - `io.github.jsonflat.schema.converter.ToString` - convert to String
  - `columnResults` - array of column objects, list of nested columns.

Translation result
star_border
### JSON schema description example
Below is a schema that outputs only those rows that have the "isbn" field, and also redefines the book price name from the full path "store_book_price" to just "price", with the field value converted to a string.
```json
{
  "name": "Example schema",
  "version": "1.0",
  "columnResults": [
    {
      "name": "store",
      "columnResults": [
        {
          "name": "book",
          "columnResults": [
            {"name": "type", "path": "category"},
            {"name": "author"},
            {"name": "title"},
            {"name": "isbn", "skipRowIfEmpty": true},
            {
              "name": "price",
              "fullname": true,
              "converter": {
                "class": "io.github.jsonflat.schema.converter.ToString"
              }
            }
          ]
        }
      ]
    },
    {
      "name": "expensive"
    }
  ]
}
```
The result of the parser will be a list of strings:
```json lines
{"store_book_type":"fiction","store_book_author":"Herman Melville","store_book_title":"Moby Dick","store_book_isbn":"0-553-21311-3","price":"8.99","expensive":10}
{"store_book_type":"fiction","store_book_author":"J. R. R. Tolkien","store_book_title":"The Lord of the Rings","store_book_isbn":"0-395-19395-8","price":"22.99","expensive":10}
```

Other examples of setting schemas via an external JSON file can be found in the test class: `io.github.jsonflat.JsonSchemaTest`

### Schema definition in application code
The schema described in the paragraph above can be defined using the code:
```java
		Schema schema = new Schema();
        schema.setColumns(
          Arrays.asList(
            new Schema.Column("store", "store", Arrays.asList(
              new Schema.Column("book", "book[*]",
                Arrays.asList(
                  new Schema.Column("type", "category", Collections.emptyList(), schema),
                  new Schema.Column("author", "author", Collections.emptyList(), schema),
                  new Schema.Column("title", "title", Collections.emptyList(), schema),
                  new Schema.Column("isbn", "isbn", Collections.emptyList(), Schema.GroupPolicy.NO_GROUP, false, false, true, Converter.DEFAULT, schema),
                  new Schema.Column("price", "price", Collections.emptyList(), Schema.GroupPolicy.NO_GROUP, true, false, false, new ToString(), schema)
                ),
                Schema.GroupPolicy.NO_GROUP, schema
              ),
              new Schema.Column("expensive", "expensive", Collections.emptyList(), schema)
            ), schema)
          )
        );
        Transformer transformer = new Transformer(schema);
        List<String> result = transformer.transform(jsonText);
```
The result of the parser will be a list of strings:
```json lines
{"store_book_type":"fiction","store_book_author":"Herman Melville","store_book_title":"Moby Dick","store_book_isbn":"0-553-21311-3","price":"8.99","expensive":10}
{"store_book_type":"fiction","store_book_author":"J. R. R. Tolkien","store_book_title":"The Lord of the Rings","store_book_isbn":"0-395-19395-8","price":"22.99","expensive":10}
```
## Working with the command line
The `io.github.jsonflat.App` class is responsible for working with the command line
To use it, you need to create a bash file
```shell
java -jar "$( dirname -- "${BASH_SOURCE[0]}" )"/jsonflat.jar "$@"
```
Utility for normalizing JSON documents from the command line.
Supports receiving and outputting data via standard input/output streams.
Supports log files on input. Every line will cut till first '[' or '{'.

Arguments:
* `-i`	Path to the input file. By default standard input.
* `-o`	Path to the output file. By default standard output.
* `-f`	If "-s" parameter is not set, defines JSONPath for filter JSON documents to processing. By default no filtering.
* `-s`	Path to parsing scheme file. By default scheme will be autogenerated by input JSON documents.
* `-d`	Custom delimiter for result columns naming. By default "_"
* `-e`	Input file encoding. By default "utf8"
* `-n`	If "-s" parameter is not set, generate scheme only by first line.
* `-a`	Explode primitive arrays to rows. By default keeps arrays as is
* `-c`	Explode arrays of objects to columns. By default explode to rows
* `-csv`	Write result in csv format. Delimiter ';'. Works fine only with -s or -n parameter. By default write in json
* `-h`	Print this help

Any other parameters define result JSON column set.

Example:
```shell
cat example.json > jsonflat store_book_title store_book_price store_bicycle*
{"store_book_title":"Sayings of the Century","store_book_price":8.95,"store_bicycle_color":"red","store_bicycle_price":19.95}
{"store_book_title":"Sword of Honour","store_book_price":12.99,"store_bicycle_color":"red","store_bicycle_price":19.95}, {"store_book_title":"Moby Dick","store_book_price":8.99,"store_bicycle_color":"red","store_bicycle_price":19.95}
{"store_book_title":"The Lord of the Rings","store_book_price":22.99,"store_bicycle_color":"red","store_bicycle_price":19.95}
```
        
        
