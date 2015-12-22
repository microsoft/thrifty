/*
 * Thrifty compiler test cases.
 */
namespace java com.bendb.thrifty.compiler.testcases

typedef string EmailAddress

const list<i32> EMPTY_INT_LIST = []
const set<double> EMPTY_DOUBLE_SET = []
const map<string, list<i32>> DUMB_CONSTANT = {
  "foo": [1, 2, 3],
  "bar": [4, 5, 6]
}

struct Email {
  1: required EmailAddress From,
  2: optional list<EmailAddress> To,
  3: optional list<EmailAddress> CC,
  4: optional list<EmailAddress> BCC,
  5: optional string Subject,
  6: optional string Body,
  7: required list<Attachment> Attachments = []
}

struct Wtf {
  1: required map<EmailAddress, ReceiptStatus> data = {"foo@bar.com": 0, "baz@quux.com": ReceiptStatus.READ}
  2: required list<map<EmailAddress, set<ReceiptStatus>>> crazy = [{"ben@thrifty.org": [1, 2]}]
}

struct NestedLists {
  1: required list<list<list<i32>>> ints = [[[4], [5]], [[], []]]
}

union Attachment {
  1: binary data,
  2: string url
}

enum ReceiptStatus {
  UNSENT,
  SENT,
  READ
}

