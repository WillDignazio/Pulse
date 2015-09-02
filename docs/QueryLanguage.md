
# Query Language

Pulse has its own query language this is used to return results. The query language is directly passed
into the backend search.

## Overview

Users can specify paramters to search for such as a given tag of documents by certain users. All paramters
are **ANDED** together to form the query. The easitest way to search is just to provide a string and
Pulse will return results that match the string.

## Lexical Syntax:
 * query  ::= tokens ',' | string
 * tokens ::= alphaNum types alphaNum tokens | nil
 * types  ::= '=' | '~' | '-=' | '-~'
