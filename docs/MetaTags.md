# Meta Tags

Meta tags are non-indexed JSON documents stored inside of Solr, and the backing
storage (eg. HBase or a SQL database). They contain pieces of information that
are used either by the Pulse application itself to "help" the indexed item
along, or for presentation at the end-user UI.

This means that while some metatags are officially supported by Pulse, others
may be solely for different UI's or end applications that treat the indexed
document differently. Given this, meta tags generally adhere to the following
rules:

1. Metatags are *NOT* guaranteed to be present
2. Metatags are *NOT* indexed by solr
3. Metatags *ARE* stored in both solr and *MAY* be in the backing storage
4. Metatags *ARE* inserted and loadable in JSON format
5. Metatags should *NOT* be considered safe against XSS _!!!_
6. Metatags do *NOT* guarantee that the document conforms to the tag*
 
 * Metatags are configured by the supplying module, thus we rely on the
 module to properly associate the metatags.

## Supported Tags:

The following list is an enumeration of the officially supported metatags that
may be found in the JSON blob:

1. _title_ -- A contiguous string, a formal title that should be used for the given result
2. _format_  -- A single word, a generic format indicator that can be used to render or associate the file in a certain situations, for instance a "image" or "file"