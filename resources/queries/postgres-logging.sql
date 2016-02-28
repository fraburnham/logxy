-- name: insert-transaction<!
   insert into transactions (uri) values (:uri);
-- name: insert-request<!
   insert into requests (transaction_id, request, body) values (:tid, :request::jsonb, :body);
-- name: insert-response<!
   insert into responses (transaction_id, response, body) values (:tid, :response::jsonb, :body);
