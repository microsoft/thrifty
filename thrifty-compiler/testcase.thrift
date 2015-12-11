/*
 * Thrifty compiler test cases.
 */
namespace java com.bendb.thrifty.compiler.testcases

typedef string EmailAddress

struct Email {
  1: required EmailAddress from,
  2: optional list<EmailAddress> to,
  3: optional list<EmailAddress> cc,
  4: optional list<EmailAddress> bcc,
  5: optional string subject,
  6: optional string body,
  7: required list<Attachment> attachments = []
}

struct Wtf {
  1: required map<EmailAddress, ReceiptStatus> data = {"foo@bar.com": 0, "baz@quux.com": READ}
  2: required list<map<EmailAddress, set<ReceiptStatus>>> crazy = [{"ben@thrifty.org": [UNSENT, SENT]}]
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

