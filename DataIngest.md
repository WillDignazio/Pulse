# Pipeline

## Modules

Client modules will send the API server index updates using the protobuf message.

#### Module Path

- Module sends request to API server
- API server sends request to HBase
- Lily indexer creates index for Solr
- API server queries Solr for element's UUID
- element's UUID is returned to the module

## HBase

Data is inserted into HBase as the reslient data storage backend. The row ID is a combination
of the module name and the unique ID given by the client. This allows easily inserting and
updating records for a given element.

#### Format

###### Column Families: data & index

###### Column qualifiers: epoch timestamp of insert

The index column family will contain an Avro record that contains all the information that is 
needed to insert into Solr. The data colum family will contain the raw payload of the item 
that is indexed. Seperating these two things allows for better performance since only one of 
them will be accessed at a time. The inddex column family will only be needed when the index
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
