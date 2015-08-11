# Pipeline

## Modules

Client modules will send the API server index updates using the protobuf message.

#### Index Pipeline

- Module sends request to API server
- API server updates HBase
  - Get the current index element, marks it as outdate, and inserts it back in
  - Inserts the new index record as the current index
- Lily indexer runs asynchronously to creates the index for Solr

## HBase

Data is inserted into HBase as the reslient data storage backend. The row ID is a combination
of the module name and the unique ID given by the module. This allows easily inserting and
updating records for a given element.

#### Format

###### Column Families: data & index

###### Column qualifiers: epoch timestamp of insert

The index column family will contain an Avro record that contains all the information that is 
needed to insert a document into Solr. The column qualifer will be the epoch timestamp with 
the exception of the newest entry which would have a column qualifer of "current". This make
it easy to identify the newest entry.


The data colum family will contain the raw payload of the item that is indexed. This will
be queried when the user wants to display stuff to the end user. Since we have the raw data,
we can do things like generating thumbnails for images, video, pdfs, etc.


Seperating these two things allows for better performance since only one of 
them will be accessed at a time. The index column family will only be needed when the index
is being generated and the data column family is only needed when the end user wants to see
the raw payload of the item that was indexed.

This will also mean that there will be a complete version of every entry at every given timestamp.
This will allow the system to return how the data looked at any given time.

#### Indexing

This will use the Lily HBase Indexer Service to provide near real-time index updates. Since
the data will be stored in HBase, we can easily recreate the index with a MR job if Solr ever
fails.

## Solr

Solr will generate an UUID for every record.
