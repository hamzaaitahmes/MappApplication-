<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');

$host = "localhost";
$db_name = "map_project";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$db_name;charset=utf8mb4", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $e) {
    echo json_encode(["success" => false, "message" => "Erreur de connexion: " . $e->getMessage()]);
    exit;
}

try {
    // Récupération des positions récentes (limite à 100 pour performance)
    $stmt = $conn->prepare(
        "SELECT id, latitude, longitude, date, imei
         FROM positions
         ORDER BY date DESC
         LIMIT 100"
    );

    $stmt->execute();
    $positions = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "success" => true,
        "positions" => $positions,
        "count" => count($positions)
    ]);
} catch(PDOException $e) {
    echo json_encode(["success" => false, "message" => "Erreur: " . $e->getMessage()]);
}
?>